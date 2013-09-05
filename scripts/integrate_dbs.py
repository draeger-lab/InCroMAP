#!/usr/bin/python
import sys
import getopt
import os.path
import codecs

def main(argv):
  
  inputfolder = ''
  outprefix = 'DATABASE'
  secondary = ''

  try:
    opts, args = getopt.getopt(argv,"i:o:s:",["ifolder=","oprefix=","secondary="])
  except getopt.GetoptError:
    print('integrate_dbs.py -i <inputfolder> -s <HMDBsecondary> [-o <outprefix>]')
    sys.exit(2)
  for opt, arg in opts:
    if opt == '-h':
      print('integrate_dbs.py -i <inputfolder> -s <HMDBsecondary> [-o <outprefix>]')
      sys.exit()
    elif opt in ("-i", "--ifolder"):
      inputfolder = arg
    elif opt in ("-o", "--oprefix"):
      outprefix = arg
    elif opt in ("-s", "--secondary"):
      secondary = arg
  
  if len(inputfolder)==0 or len(secondary)==0:
    print('integrate_dbs.py -i <inputfolder> -s <HMDBsecondary> [-o <outprefix>]')
    sys.exit(2)
   
  
  print(''.join(['Reading data from folder ',inputfolder]))
  print(''.join(['Writing to ',outprefix]))
  
  if not os.path.exists(inputfolder):
    print('Inputfolder does not exist!')
    sys.exit(2)
  
  files = os.listdir(inputfolder)
  print(''.join(['Found ',str(len(files)),' information files']))
  
  entries = []
  for f in files:
    print(''.join(['Reading ',f]))
    entries = entries + read_file(os.path.join(inputfolder,f))
  
  print(''.join(['Found total number of ', str(len(entries)),' entries.']))
  
  tables = {}
  tables['HMDB'] = gen_table('HMDB',entries)
  tables['LMID'] = gen_table('LMID',entries)
  tables['Kegg'] = gen_table('Kegg',entries)
  tables['CHEBI'] = gen_table('CHEBI',entries)
  tables['PC_compound'] = gen_table('PC_compound',entries)
  tables['InChIKey'] = gen_table('InChIKey',entries)
  
  prim_table = integrate_tables(tables,'InChIKey')
  

def read_file(f):
  fh = open(f,mode='r')
  entries = list()
  
  header = fh.readline().strip().split('\t')
  
  for line in fh:
    entries.append(parse_line(line,header))
  
  return entries

def parse_line(line,header):
  entry = {}
  fields = line.split('\t')
  
  i=0
  for s in fields:
    if len(s.strip())>0:
      entry[header[i]]=s.strip()
    i=i+1
  
  return entry

def gen_table(ident,entries):
  mappings = {}
  
  for entry in entries:
    if ident in entry:
      if entry[ident] in mappings:
        mappings[entry[ident]].append(entry)
      else:
        mappings[entry[ident]]=[entry]
  
  return mappings

def integrate_tables(tables,primary):
  t_prim = tables[primary]
  
  for t_key in tables:
    if t_key == 'HMDB':
      table = tables[t_key]
      for key in table:
        entryset = process_entry(table[key],tables,set())
        if len(entryset)>1:
          print entryset 
          break;
      break;
        
def process_entry(entry,tables,visited=set()):
  
  l = len(visited)
  
  for e in entry:
    fe = frozenset(e.items())
    if fe not in visited:
      visited.add(fe)
  
  if l == len(visited):
    return visited
  
  for e in entry:
    for (k,v) in e.items():
      if k in tables:
        enew = tables[k][v]
        visited = visited & process_entry(enew,tables,visited)
  
  return visited
  
  
  
if __name__ == "__main__":
  main(sys.argv[1:])
