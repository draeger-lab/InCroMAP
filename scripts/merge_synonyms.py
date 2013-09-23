#!/usr/bin/python
import sys
import getopt
import os
import codecs
import re


def main(argv):
  
  lmdbsynonyms = ''
  hmdbsynonyms = ''
  pcsynonyms = ''
  compounddb = ''

  try:
    opts, args = getopt.getopt(argv,"l:h:p:c:",["lmdb=","hmdb=","pc=","cpddb="])
  except getopt.GetoptError:
    print('merge_synonyms.py -l <lmdb> -h <hmdb> -p <pc> -c <compoundFile>')
    sys.exit(2)
  for opt, arg in opts:
    if opt == '--help':
      print('merge_synonyms.py -l <lmdb> -h <hmdb> -p <pc> -c <compoundFile>')
      sys.exit()
    elif opt in ("-l", "--lmdb"):
      lmdbsynonyms = arg
    elif opt in ("-h", "--hmdb"):
      hmdbsynonyms = arg
    elif opt in ("-p", "--pc"):
      pcsynonyms = arg
    elif opt in ("-c", "--cpddb"):
      compounddb = arg
  
  print(''.join(['Reading synonym files: LMDB: ',lmdbsynonyms,', PubChem ', pcsynonyms,' and HMDB: ', hmdbsynonyms]))
  print(''.join(['Reading form CompoundDB file ',compounddb]))
  
  if not (os.path.exists(lmdbsynonyms) and os.path.exists(hmdbsynonyms) and os.path.exists(pcsynonyms) and os.path.exists(compounddb)):
    print('At least one of the input files does not exist!')
    sys.exit(2)
  
  o_synonyms = codecs.open("CompoundSynonyms.txt",'w', 'utf-8')
  o_synonyms.write('InChIKey\tSynonym1\tSynonym2\t...\n')
  o_metab = codecs.open("CompoundData.txt",'w', 'utf-8')
  o_metab.write('InChIKey\tName\tHMDB\tLMID\tKegg\tCHEBI\tPC_compound\n')
  
  lmdbmap = read_synonymfile(lmdbsynonyms)
  pcmap = read_synonymfile(pcsynonyms)
  hmdbmap = read_synonymfile(hmdbsynonyms)
  
  f = open(compounddb,mode='r')
  
  header = f.readline().strip().split('\t')
  for line in f:
    synonyms = set()
    metabolite = parse_line(line,header)
    if "Name" in metabolite:
      synonyms.add(metabolite["Name"])
    if "HMDB" in metabolite:
      h_names = get_synonyms(metabolite["HMDB"], hmdbmap)
      synonyms = synonyms.union(h_names)
    if "PC_compound" in metabolite:
      pc_names = get_synonyms(metabolite["PC_compound"], pcmap)
      synonyms = synonyms.union(pc_names)
    if "LMID" in metabolite:
      lmdb_names = get_synonyms(metabolite["LMID"], lmdbmap)
      synonyms = synonyms.union(lmdb_names)
    if "Name" not in metabolite and len(synonyms)>0:
      s = select_synonym(synonyms)
      metabolite["Name"]=s
    if len(synonyms)>0:
      write_syn(o_synonyms,metabolite['InChIKey'],synonyms)
    write_metab(o_metab,metabolite)
    
  o_synonyms.flush()
  o_synonyms.close()
  o_metab.flush()
  o_metab.close()
  f.close()


def read_synonymfile(f):
  fh = open(f,mode='r')
  mapping = {}
  
  #we do not need the header line
  fh.readline()
  
  for line in fh:
    fields = line.split('\t')
    synonymList = list()
    for i in range(1,len(fields)-1):
      synonymList.append(fields[i].strip())
    mapping[fields[0].strip()]=synonymList
  
  return mapping

def parse_line(line,header):
  entry = {}
  fields = line.split('\t')
  
  i=0
  for s in fields:
    if len(s.strip())>0:
      if '|' in  s:
        entry[header[i]]=s.strip().split('||')
      else:
        entry[header[i]]=s.strip()
    i=i+1
  
  return entry

def get_synonyms(idlist,mapping):
  synonyms = set()
  if isinstance(idlist,str):
    idlist = [idlist]
    
  for i in idlist:
    if not i in mapping:
      continue
    names = mapping[i]
    for name in names:
      if name not in synonyms:
        synonyms.add(name)
  return synonyms

def write_syn(fh,inchi,synonyms):
  fh.write(inchi)
  for s in synonyms:
    fh.write('\t')
    fh.write(s)
  fh.write('\n')
  
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
  
def select_synonym(synonyms):
  l = sys.maxint
  s = ""
  for syn in synonyms:
    if len(syn)<l:
      l=len(syn)
      s = syn
  return s

if __name__ == "__main__":
  main(sys.argv[1:])
