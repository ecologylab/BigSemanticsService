package ecologylab.bigsemantics.dpool;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import ecologylab.bigsemantics.httpclient.SimplHttpResponse;
import ecologylab.io.StreamUtils;
import ecologylab.serialization.annotations.simpl_inherit;
import ecologylab.serialization.annotations.simpl_scalar;

/**
 * 
 * @author quyin
 */
@simpl_inherit
public class RemoteCurlDownloader extends Downloader
{

  static Logger                       logger = LoggerFactory.getLogger(RemoteCurlDownloader.class);

  @simpl_scalar
  private int                         port   = 22;

  @simpl_scalar
  private String                      user;

  @simpl_scalar
  private String                      password;

  @simpl_scalar
  private String                      keyPath;

  @simpl_scalar
  private String                      passPhrase;

  @simpl_scalar
  private int                         connectTimeout;

  private JSch                        jsch;

  private Properties                  sessionConfig;

  private ArrayBlockingQueue<Session> sessions;

  /**
   * For deserialization only.
   */
  public RemoteCurlDownloader()
  {
    super();
  }

  public RemoteCurlDownloader(String host, int numThreads)
  {
    this(host, numThreads, 0);
  }

  public RemoteCurlDownloader(String host, int numThreads, int priority)
  {
    super(host, numThreads, priority);
  }

  public String getHost()
  {
    return getId();
  }

  public void setHost(String host)
  {
    super.setId(host);
  }

  public int getPort()
  {
    return port;
  }

  public void setPort(int port)
  {
    this.port = port;
  }

  public String getUser()
  {
    return user;
  }

  public void setUser(String user)
  {
    this.user = user;
  }

  public String getPassword()
  {
    return password;
  }

  public void setPassword(String password)
  {
    this.password = password;
  }

  public String getKeyPath()
  {
    return keyPath;
  }

  public void setKeyPath(String keyPath)
  {
    this.keyPath = keyPath;
  }

  public String getPassPhrase()
  {
    return passPhrase;
  }

  public void setPassPhrase(String passPhrase)
  {
    this.passPhrase = passPhrase;
  }

  public int getConnectTimeout()
  {
    return connectTimeout;
  }

  public void setConnectTimeout(int connectTimeout)
  {
    this.connectTimeout = connectTimeout;
  }

  public int initialize() throws Exception
  {
    jsch = new JSch();

    // set up host, user, and authentication
    String host = getHost();
    if (host == null || user == null)
    {
      throw new DpoolException("Remote downloader: Missing host or user name");
    }
    if (password == null && keyPath == null)
    {
      throw new DpoolException("Remote downloader: Missing password or public key");
    }
    if (password == null)
    {
      if (passPhrase == null || passPhrase.isEmpty())
      {
        jsch.addIdentity(keyPath);
      }
      else
      {
        jsch.addIdentity(keyPath, passPhrase);
      }
    }

    sessionConfig = new Properties();
    sessionConfig.put("StrictHostKeyChecking", "no");

    // create sessions
    sessions = new ArrayBlockingQueue<Session>(getNumThreads());
    for (int i = 0; i < getNumThreads(); ++i)
    {
      Session session = createSession();
      session.connect(connectTimeout);
      sessions.put(session);
    }

    ExecResult curlTest = execCommand("command -v curl 2>&1 >/dev/null");
    if (curlTest.exitCode == 0)
    {
      logger.debug("Connected to " + host);
    }
    else
    {
      throw new DpoolException("curl not supported on remote machine " + host);
    }

    logger.debug("{} sessions created for {}@{}", sessions.size(), user, host);
    return sessions.size();
  }

  private Session createSession() throws JSchException
  {
    Session session = null;
    String host = getHost();
    if (password == null)
    {
      session = jsch.getSession(user, host, port);
    }
    else
    {
      session = jsch.getSession(user, host, port);
      session.setPassword(password);
    }
    session.setConfig(sessionConfig);
    return session;
  }

  public ExecResult execCommand(String command)
      throws InterruptedException, JSchException, IOException
  {
    long t0 = System.currentTimeMillis();
    Session session = null;
    ChannelExec channel = null;
    int exitCode = 0;
    try
    {
      session = sessions.take();
      if (!session.isConnected())
      {
        session = createSession();
        session.connect(connectTimeout);
      }

      channel = (ChannelExec) session.openChannel("exec");
      channel.setCommand(command);
      channel.setInputStream(null);
      InputStream channelOut = channel.getInputStream();
      InputStream channelErr = channel.getErrStream();

      channel.connect();
      ByteArrayOutputStream outStream = new ByteArrayOutputStream();
      StreamUtils.copyInputStream(channelOut, outStream);
      byte[] out = outStream.toByteArray();
      ByteArrayOutputStream errStream = new ByteArrayOutputStream();
      StreamUtils.copyInputStream(channelErr, errStream);
      byte[] err = errStream.toByteArray();
      exitCode = channel.getExitStatus();

      return new ExecResult(out, err, exitCode);
    }
    finally
    {
      long t = System.currentTimeMillis() - t0;
      logger.debug("{}@{}: {} (exit code: {}; time: {})", user, getHost(), command, exitCode, t);
      if (channel != null && channel.isConnected())
      {
        channel.disconnect();
      }
      if (session != null)
      {
        sessions.put(session);
      }
    }
  }

  protected class ExecResult
  {
    byte[] out;

    byte[] err;

    int    exitCode;

    public ExecResult(byte[] out, byte[] err, int exitCode)
    {
      super();
      this.out = out;
      this.err = err;
      this.exitCode = exitCode;
    }
  }

  public void disconnectAll()
  {
    if (sessions != null)
    {
      for (Session session : sessions)
      {
        if (session.isConnected())
        {
          session.disconnect();
        }
      }
    }
  }

  @Override
  public int doPerformDownload(DownloadTask task) throws Exception
  {
    String userAgent = task.getUserAgent();
    String url = task.getUrl();
    String command = String.format("curl -i -L -A \"%s\" \"%s\"", userAgent, url);
    ExecResult execResult = null;
    try
    {
      execResult = execCommand(command);
    }
    catch (JSchException e)
    {
      incConsecutiveFailures();
      throw e;
    }
    catch (IOException e)
    {
      incConsecutiveFailures();
      throw e;
    }

    int exitCode = execResult.exitCode;
    if (exitCode == 0)
    {
      ByteArrayInputStream istream = new ByteArrayInputStream(execResult.out);
      SimplHttpResponse resp = SimplHttpResponse.parse(url, istream);
      task.setResponse(resp);
      return resp.getCode();
    }
    else
    {
      String errString = new String(execResult.err, Charset.forName("UTF-8"));
      throw new DpoolException("Failed to exec " + command + " on " + getHost() + ";"
                               + " exit code = " + exitCode + ", error = " + errString);
    }
  }

  public void copyFrom(RemoteCurlDownloader other)
  {
    if (this.getNumThreads() == 0)
    {
      this.setNumThreads(other.getNumThreads());
    }
    if (this.port == 0)
    {
      this.port = other.port;
    }
    if (this.user == null)
    {
      this.user = other.user;
    }
    if (this.password == null)
    {
      this.password = other.password;
    }
    if (this.keyPath == null)
    {
      this.keyPath = other.keyPath;
    }
    if (this.passPhrase == null)
    {
      this.passPhrase = other.passPhrase;
    }
    if (this.connectTimeout == 0)
    {
      this.connectTimeout = other.connectTimeout;
    }
  }

}
