#!python

import sys
import re

p_loc = re.compile('"document_url":"([^"]+)"')
p_ms_d = re.compile('"ms_html_download":(\d+)')
p_ms_ext = re.compile('"ms_extraction":(\d+)')
p_ms_ttl = re.compile('"ms_total":(\d+)')
# p_pk_ints = re.compile('"peek_interval":\[("\d+"(,"\d+")*)\]')

p_page_cache_lookup = re.compile('"ms_page_cache_lookup":(\d+)')
p_page_caching = re.compile('"ms_page_caching":(\d+)')
p_page_local_file_connecting = re.compile('"ms_page_local_file_connecting":(\d+)')
p_content_reading_and_dom_creation = re.compile('"ms_content_reading_and_dom_creation":(\d+)')
p_content_body_and_clippings = re.compile('"ms_content_body_and_clippings":(\d+)')
p_image_text_parser_calling_super_parse = re.compile('"ms_image_text_parser_calling_super_parse":(\d+)')
p_metadata_cache_lookup = re.compile('"ms_metadata_cache_lookup":(\d+)')
p_metadata_caching = re.compile('"ms_metadata_caching":(\d+)')



def extract(pattern, line, group_index, default_value = None):
  m = pattern.search(line)
  if m is not None:
    return m.group(group_index)
  return default_value

if len(sys.argv) <= 1:
  print 'Usage: {0} <perf_log_file>'.format(sys.argv[0])
  quit()

# print '#url\tms_d\tms_page_cache_lookup\tms_page_caching\tms_local_file\tms_ext\tms_read_dom\tms_body_clippings\tms_super_parse\tms_md_cache_lookup\tms_md_caching\tq_ttl\tms_ttl\tpk_ints'
print '#url,pc_download,pc_extraction,pc_dne,ms_total'

fn = sys.argv[1]
f = open(fn)
lines = f.readlines()
for line in lines:
  loc = extract(p_loc, line, 1, None)
  if loc is not None:
    loc = re.sub('\\\\\\/', '/', loc)

  ms_d = extract(p_ms_d, line, 1, '0')
  ms_ext = extract(p_ms_ext, line, 1, '0')
  ms_ttl = extract(p_ms_ttl, line, 1, '0')
#  s_pk_ints = extract(p_pk_ints, line, 1, '')
#  pk_ints = [s for s in re.findall(r'\d+', s_pk_ints)]
#  q_ttl = pk_ints[-1] if len(pk_ints) > 0 else '0'

  ms_page_cache_lookup = extract(p_page_cache_lookup, line, 1, '0')
  ms_page_caching = extract(p_page_caching, line, 1, '0')
  ms_page_local_file_connecting = extract(p_page_local_file_connecting, line, 1, '0')
  ms_content_reading_and_dom_creation = extract(p_content_reading_and_dom_creation, line, 1, '0')
  ms_content_body_and_clippings = extract(p_content_body_and_clippings, line, 1, '0')
  ms_image_text_parser_calling_super_parse = extract(p_image_text_parser_calling_super_parse, line, 1, '0')
  ms_metadata_cache_lookup = extract(p_metadata_cache_lookup, line, 1, '0')
  ms_metadata_caching = extract(p_metadata_caching, line, 1, '0')

  if loc is not None and line.find('cache_hit') < 0:
    print '"{}", {:.2%}, {:.2%}, {:.2%}, {}'.format(
      loc,
      float(ms_d)/float(ms_ttl),
      float(ms_ext)/float(ms_ttl),
      (float(ms_d) + float(ms_ext))/float(ms_ttl),
      ms_ttl)
  continue

  if loc is not None:
    print '{0}\t{1}\t{2}\t{3}\t{4}\t{5}\t{6}\t{7}\t{8}\t{9}\t{10}\t{11}'.format(
      loc,
      ms_d,
      ms_page_cache_lookup,
      ms_page_caching,
      ms_page_local_file_connecting,
      ms_ext,
      ms_content_reading_and_dom_creation,
      ms_content_body_and_clippings,
      ms_image_text_parser_calling_super_parse,
      ms_metadata_cache_lookup,
      ms_metadata_caching,
      ms_ttl)

