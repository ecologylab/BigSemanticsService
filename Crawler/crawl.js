// Script for crawling scholarly articles and their authors, references, and
// citations.

var EOL = require('os').EOL;

function default_error_handler(err, result) {
  if (err) {
    console.log("ERROR: " + err);
  } else if (result) {
    console.log("RESULT: " + result);
  }
}

// database related functions

var db_file_name = "urls.db";
var sqlite3 = require("sqlite3");
var db = new sqlite3.Database(db_file_name);

// callback :: (err, url) -> nil
function next_url(callback) {
  db.get("SELECT url FROM urls ORDER BY rowid LIMIT 1;", function(err, row) {
    if (err) {
      callback("cannot read table 'urls': " + err, null);
    } else {
      if (row) {
        callback(null, row.url);
        var stmt = db.prepare("DELETE FROM urls WHERE url = ?;");
        stmt.run(row.url, default_error_handler);
        stmt.finalize();
      } else {
        callback("no more URLs to process", null);
      }
    }
  });
}

// callback :: (err, url) -> nil
// where url will be the unvisited url, or null
function is_visited(url, callback) {
  var stmt = db.prepare("SELECT url FROM visited_urls WHERE url == ? LIMIT 1;");
  stmt.get(url, function(err, row) {
    if (err) {
      callback("cannot query table 'visited_urls': " + err, null);
    } else {
      if (row) {
        callback("URL was visited: " + url, null);
      } else {
        callback(null, url);
      }
    }
  });
  stmt.finalize();
}

function new_url(url) {
  console.log("  adding link: " + url);
  var stmt = db.prepare("INSERT INTO urls VALUES(?, NULL);");
  stmt.run(url, default_error_handler);
  stmt.finalize();
}

// metadata related functions

var request = require("request");
var user_agent = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36"
                 + " (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36";
var bss_base = "http://api.ecologylab.net/BigSemanticsService";

// callback :: (err, metadata) -> nil
function get_metadata(url, callback) {
  var options = {
    "method": "GET",
    "url": url,
    "headers": {
      "User-Agent": user_agent,
    },
  };

  var result = {};
  request(options, function(error, resp, body) {
    if (error) {
      callback("cannot extract metadata from " + url + ": " + error, null);
    } else {
      try {
        var obj = JSON.parse(body);
        var names = Object.keys(obj);
        if (names.length > 0) {
          var name = names[0];
          if (obj[name]) {
            callback(null, obj[name]);
            return;
          }
        }
        callback("cannot parse metadata from " + url + ": "
                 + JSON.stringify(obj),
                 null);
      } catch (exception) {
        callback("cannot parse metadata from " + url + ": "
                 + exception
                 + "; raw JSON: " + body,
                 null);
      }
    }
  });
}

// callback :: (err, normalized_url) -> nil
function normalize(doc_url, callback) {
  var bss_url = bss_base + "/metadata_or_stub.json?url=" + encodeURIComponent(doc_url);
  get_metadata(bss_url, function(err, metadata) {
    if (err) {
      callback("cannot normalize " + doc_url + ": " + err, null);
    } else {
      var normalized_url = metadata.location;
      if (normalized_url) {
        callback(null, normalized_url);
      } else {
        callback("cannot find location from " + JSON.stringify(metadata)
                 + " when normalizing " + doc_url,
                 null);
      }
    }
  });
}

function get_links(metadata) {
  var result = [];

  // authors
  if (metadata.authors) {
    for (var i in metadata.authors) {
      if (metadata.authors[i].location) {
        result.push(metadata.authors[i].location);
      }
    }
  }

  // references
  if (metadata.references) {
    for (var i in metadata.references) {
      if (metadata.references[i].location) {
        result.push(metadata.references[i].location);
      }
    }
  }

  // citations
  if (metadata.citations) {
    for (var i in metadata.citations) {
      if (metadata.citations[i].location) {
        result.push(metadata.citations[i].location);
      }
    }
  }

  // creative_works
  if (metadata.creative_works) {
    for (var i in metadata.creative_works) {
      if (metadata.creative_works[i].location) {
        result.push(metadata.creative_works[i].location);
      }
    }
  }

  return result;
}

// callback :: (err, visited_url) -> nil
function visit(doc_url, callback) {
  var bss_url = bss_base + "/metadata.json?url=" + encodeURIComponent(doc_url);
  get_metadata(bss_url, function(err, metadata) {
    if (err) {
      callback("cannot visit " + doc_url + ": " + err, null);
    } else {
      var visited_url = metadata.location;
      if (visited_url) {
        var links = get_links(metadata);
        for (var i in links) {
          new_url(links[i]);
        }
        callback(null, visited_url);
      } else {
        callback("cannot find location from " + JSON.stringify(metadata)
                 + " when visiting " + doc_url,
                 null);
      }
    }
  });
}

// glue things together in a monadic way

// lift :: (err, b) -> (a, (err, b))
function lift(f) {
  return function(placeholder, callback) {
    f(callback);
  }
}

// bind :: ( (b, (err, c)) -> nil, (a, (err, b)) -> nil )
//         -> ( (a, (err, c)) -> nil )
function bind(f, g) {
  return function(gin, callback) {
    g(gin, function(err, gout) {
      if (err) {
        console.log("ERROR: " + err);
      } else {
        f(gout, callback);
      }
    });
  };
}

function finish_url(error, url) {
  if (error) {
    console.log("ERROR: failed to visit " + url
                + ", putting back to task queue...");
    var stmt = db.prepare("INSERT INTO urls VALUES (?, \"retry\");");
    stmt.run(url, default_error_handler);
    stmt.finalize();
  } else {
    console.log("successfully processed " + url);
    var stmt = db.prepare("INSERT INTO visited_urls VALUES (?, ?, NULL);");
    stmt.run(url, new Date(), default_error_handler);
    stmt.finalize();
  }
  console.log(EOL);
  console.log(EOL);
}

var process_one = bind(visit,
                  bind(is_visited,
                  bind(normalize, lift(next_url))));

function main() {
  function tick() {
    process_one(null, function(err, url) {
      if (err) {
        if (err.indexOf("no more URLs to process") >= 0) {
          process.exit(0);
        } else {
          console.log("ERROR: " + err + ", putting back to task queue...");
          var stmt = db.prepare("INSERT INTO urls VALUES (?, \"retry\");");
          stmt.run(url, default_error_handler);
          stmt.finalize();
        }
      } else {
        console.log("successfully processed " + url);
        var stmt = db.prepare("INSERT INTO visited_urls VALUES (?, ?, NULL);");
        stmt.run(url, new Date(), default_error_handler);
        stmt.finalize();
      }
      process.nextTick(tick);
    });
  }

  process.nextTick(tick);
}

main()

