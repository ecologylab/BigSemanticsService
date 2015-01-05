#!/usr/bin/ruby

require 'rubygems'
require 'uri'
require 'open-uri'
require 'nokogiri'
require 'thread'

def expandLoc(q, loc)
	locUri = URI.parse(loc)

	# Get the HTML input as a Nokogiri parsed document
	# TODO error checking!
	# TODO redirect?
	doc = Nokogiri::HTML(open(loc))

	# Print out each anchor location
	doc.xpath('.//a/@href').each do |anchor_loc|
		if anchor_loc.value.start_with? "/"
			newLoc = "#{locUri.scheme}//#{locUri.host}#{anchor_loc.value}"
		else
			newLoc = anchor_loc.value
		end
		q.push(newLoc)
	end
end

seed = "http://google.com/search?q=tenderlove"
n = 5

queue = Queue.new
queue.push(seed)

i = 0
until queue.empty? do
	loc = queue.pop(true) rescue nil
	if loc
		puts loc
		i += 1
		if i >= n
			break
		end
		expandLoc(queue, loc)
	end
end

