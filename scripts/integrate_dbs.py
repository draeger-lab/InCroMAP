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

   
  inchitable = gen_table('InChIKey',entries)
  
  integrated_table = integrate_tables(inchitable)
  
  o_m = codecs.open(outprefix+".txt",'w', 'utf-8')
  o_m.write('InChIKey\tName\tHMDB\tLMID\tKegg\tCHEBI\tPC_compound\n')

  
  for m in integrated_table:
    write_metab(o_m,m)
    
  o_m.flush()
  o_m.close()
 

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
      if '|' in  s:
        entry[header[i]]=s.strip().split('|')
      else:
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

def integrate_tables(table):
  metablist =[]
  for (k,v) in table.items():
    metab = v[0]
    if len(v)>1:
      metab = merge_entries(k,v)
    metablist.append(metab)
    
  return metablist

def merge_entries(inchik,entry):
  metabolite = {}
  
  for e in entry:
    for (k,v) in e.items():
      if k not in metabolite:
        metabolite[k]=v
      else:
        if metabolite[k]==v:
          continue
        else:
          metabolite[k] = solve_ambiguity(k,v,metabolite[k])
          #if isinstance(metabolite[k],list):
          #  print k,inchik,','.join(metabolite[k])
    
  return metabolite

def solve_ambiguity(k, val1, val2):
  if isinstance(val1, list) or isinstance(val2,list):
    if not isinstance(val1,list):
      val1 = [val1]
    if not isinstance(val2,list):
      val2 = [val2]
    res = list(set(val1 + val2))
    return res
  else:
    if val1.upper() == val2.upper():
      return val1
    if val1.upper() in val2.upper():
      return val1
    if val2.upper() in val1.upper():
      return val2
    if k=='Name':
      if len(val1)<len(val2):
        return val1
      else:
        return val2
    return [val1,val2]

def write_metab(fh,metabolite):
  fh.write(metabolite['InChIKey'])
  fh.write('\t')
  if 'Name' in metabolite:
    write_entry(fh,metabolite['Name'])
  fh.write('\t')
  if 'HMDB' in metabolite:
    write_entry(fh,metabolite['HMDB'])
  fh.write('\t')
  if 'LMID' in metabolite:
    write_entry(fh,metabolite['LMID'])
  fh.write('\t')
  if 'Kegg' in metabolite:
    write_entry(fh,metabolite['Kegg'])
  fh.write('\t')
  if 'CHEBI' in metabolite:
    write_entry(fh,metabolite['CHEBI'])
  fh.write('\t')
  if 'PC_compound' in metabolite:
    write_entry(fh,metabolite['PC_compound'])
  fh.write('\n')
  
def write_entry(fh,e):
  if isinstance(e,list):
    fh.write('||'.join(e))
  else:
    fh.write(e)

if __name__ == "__main__":
  main(sys.argv[1:])
