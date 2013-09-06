#!/usr/bin/python
import sys
import getopt
import os
import codecs
import re


def main(argv):
  
  inputfile = ''
  outprefix = ''

  try:
    opts, args = getopt.getopt(argv,"i:o:",["ifolder=","oprefix="])
  except getopt.GetoptError:
    print('read_lmsddump.py -i <input> -o <outprefix>')
    sys.exit(2)
  for opt, arg in opts:
    if opt == '-h':
      print('read_lmsddump.py -i <input> -o <outprefix>')
      sys.exit()
    elif opt in ("-i", "--ifile"):
      inputfile = arg
    elif opt in ("-o", "--oprefix"):
      outprefix = arg
  
  print(''.join(['Reading from file ',inputfile]))
  print(''.join(['Writing to ',outprefix]))
  
  if not os.path.exists(inputfile):
    print('Compound input file does not exist!')
    sys.exit(2)
  
  f = open(inputfile,mode='r')
  
  o_pc = codecs.open(outprefix+".txt",'w', 'utf-8')
  o_pc.write('PC_compound\tHMDB\tKegg\tCHEBI\tLMID\n')
  
  for c in read_compounds(f):
    write_metab(o_pc,c)
    
  o_pc.flush()
  o_pc.close()
    
  
def read_compounds(f):
  startline = f.readline().split('\t')
  compound = dict()
  compound['id']=startline[0].strip()
  
  entry = check_entry(startline[1])
  if entry[0] is not None:
    compound[entry[0]]=list()
    compound[entry[0]].append(entry[1])
  
  for line in f:
    l = line.split('\t')
    if compound['id'] != l[0].strip():
      yield compound
      compound = dict()
      compound['id']=l[0].strip()
  
    entry = check_entry(l[1])
    key = entry[0]
    val = entry[1]
    
    if key is not None:
      if not key in compound:
        compound[key]=list()
      compound[key].append(val)
  

def check_entry(st):
  s = st.strip();
  if s.startswith('HMDB'):
    return 'hmdb',s
  if s.startswith('CHEBI'):
    return 'chebi',s.replace('CHEBI:','')
  if re.match('^LM(FA|GL|GP|SP|ST|PR|SL|PK)[0-9]{4}([0-9a-zA-Z]{4,6})?$',s) is not None:
    return 'lmid',s
  if re.match('C\d+$',s) is not None:
    return 'kegg',s
  return None,None
  
def write_metab(fh,metabolite):
  if len(metabolite.keys())==1:
    return
  fh.write(metabolite['id'])
  fh.write('\t')
  if 'hmdb' in metabolite:
    fh.write("|".join(metabolite['hmdb']))
  fh.write('\t')
  if 'kegg' in metabolite:
    fh.write("|".join(metabolite['kegg']))
  fh.write('\t')
  if 'chebi' in metabolite:
    fh.write("|".join(metabolite['chebi']))
  fh.write('\t')
  if 'lmid' in metabolite:
    fh.write("|".join(metabolite['lmid']))
  fh.write('\n')

if __name__ == "__main__":
  main(sys.argv[1:])
