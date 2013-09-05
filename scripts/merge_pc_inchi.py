#!/usr/bin/python
import sys
import getopt
import os
import codecs
import re


def main(argv):
  
  inchifile = ''
  pcfile = ''
  meshfile = ''
  outprefix = ''

  try:
    opts, args = getopt.getopt(argv,"i:p:m:o:",["inchi=","pcfile=","meshfile=","oprefix="])
  except getopt.GetoptError:
    print('merge_pc_inchi.py -i <inchi> -p <pcfile> -m <meshfile> -o <outprefix>')
    sys.exit(2)
  for opt, arg in opts:
    if opt == '-h':
      print('merge_pc_inchi.py -i <inchi> -p <pcfile> -m <meshfile> -o <outprefix>')
      sys.exit()
    elif opt in ("-i", "--inchi"):
      inchifile = arg
    elif opt in ("-p", "--pcfile"):
      pcfile = arg
    elif opt in ("-m", "--meshfile"):
      meshfile = arg
    elif opt in ("-o", "--oprefix"):
      outprefix = arg
  
  print(''.join(['Reading inchi file ',inchifile,', pcfile ', pcfile,' and meshfile ', meshfile]))
  print(''.join(['Writing to ',outprefix]))
  
  if not os.path.exists(inchifile):
    print('Compound input file does not exist!')
    sys.exit(2)
  
  o_pc = codecs.open(outprefix+".txt",'w', 'utf-8')
  o_pc.write('InChIKey\tName\tHMDB\tLMID\tPC_compound\tKegg\tCHEBI\n')
  
  inchimap = read_inchifile(inchifile)
  meshmap = read_meshfile(meshfile)
  
  f = open(pcfile,mode='r')
  f.readline()
  
  for line in f:
    metabolite = parse_line(line,inchimap,meshmap)
    write_metab(o_pc,metabolite)
    
  o_pc.flush()
  o_pc.close()
  f.close()


def read_inchifile(f):
  fh = open(f,mode='r')
  mapping = {}
  
  header = fh.readline().strip().split('\t')
  
  for line in fh:
    fields = line.split('\t')
    mapping[fields[0].strip()]=fields[1].strip()
  
  return mapping

def read_meshfile(f):
  fh = open(f,mode='r')
  mapping = {}
  
  header = fh.readline().strip().split('\t')
  
  for line in fh:
    fields = line.split('\t')
    mapping[fields[0].strip()]=fields[1].strip()
  
  return mapping

def parse_line(line,inchimapping,meshmapping):
  metabolite = dict()
  s = line.rstrip('\n').split('\t')

  metabolite['pc_compound']=s[0].strip();
  metabolite['hmdb']=s[1].strip();
  metabolite['kegg']=s[2].strip();
  metabolite['chebi']=s[3].strip();
  metabolite['lmid']=s[4].strip();
  metabolite['inchikey']=inchimapping[metabolite['pc_compound']];
  if metabolite['pc_compound'] in meshmapping:
    metabolite['name']=meshmapping[metabolite['pc_compound']];
  
  return metabolite

def write_metab(fh,metabolite):
  if 'inchikey' in metabolite:
    fh.write(metabolite['inchikey'])
  fh.write('\t')
  if 'name' in metabolite:
    fh.write(metabolite['name'])
  fh.write('\t')
  if 'hmdb' in metabolite:
    fh.write(metabolite['hmdb'])
  fh.write('\t')
  if 'lmid' in metabolite:
    fh.write(metabolite['lmid'])
  fh.write('\t')
  if 'kegg' in metabolite:
    fh.write(metabolite['kegg'])
  fh.write('\t')
  if 'chebi' in metabolite:
    fh.write(metabolite['chebi'])
  fh.write('\t')
  fh.write(metabolite['pc_compound'])
  fh.write('\n')

if __name__ == "__main__":
  main(sys.argv[1:])
