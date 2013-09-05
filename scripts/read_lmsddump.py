#!/usr/bin/python
import sys
import getopt
import os
import codecs

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
  
  header = f.readline().split('\t')
    
  pos_LMID =  header.index('LM_ID')
  pos_Name = header.index('COMMON_NAME')
  pos_Synonyms = header.index('SYNONYMS')
  pos_PubChem = header.index('PUBCHEM_CID')
  pos_kegg = header.index('KEGG_ID')
  pos_hmdb = header.index('HMDBID')
  pos_chebi = header.index('CHEBI_ID')
  pos_inchikey = header.index('INCHI_KEY')
  
  positions = [pos_LMID,pos_Name,pos_hmdb,pos_PubChem,pos_kegg,pos_chebi,pos_inchikey,pos_Synonyms]
  
  o_synonyms =  codecs.open(outprefix + '.synonyms','w', 'utf-8')
  o_synonyms.write('LMID\tSynonym1\tSynonym2\t...\n')
  o_lmdb = codecs.open(outprefix+".txt",'w', 'utf-8')
  o_lmdb.write('InChIKey\tName\tHMDB\tLMID\tKegg\tCHEBI\tPC_compound\n')
  
  for line in f:
    metabolite = parse_line(line,positions)
    write_metab(o_lmdb,metabolite)
    write_synonyms(o_synonyms,metabolite)
    
  o_synonyms.flush()
  o_lmdb.flush()
  o_synonyms.close()
  o_lmdb.close()
  
def parse_line(line,positions):
  metabolite = dict()
  s = line.split('\t')
  
  metabolite['lmid']=s[positions[0]].strip();
  metabolite['name']=s[positions[1]].strip();
  metabolite['hmdb']=s[positions[2]].strip();
  metabolite['pubchem_compound']=s[positions[3]].strip();
  metabolite['kegg']=s[positions[4]].strip();
  metabolite['chebi']=s[positions[5]].strip();
  metabolite['inchikey']=s[positions[6]].strip();
  metabolite['synonyms']=s[positions[7]].strip().split(';');
  
  return metabolite
    
  
def write_synonyms(fh,metabolite):
  fh.write(metabolite['lmid'])
  for s in metabolite['synonyms']:
    #s = s.encode(encoding='utf_8', errors='replace')
    fh.write('\t')
    fh.write(s.strip())
  fh.write('\n')
  
def write_metab(fh,metabolite):
  if metabolite['inchikey'] is not None:
    fh.write(metabolite['inchikey'].strip())
  fh.write('\t')
  fh.write(metabolite['name'])
  fh.write('\t')
  if(metabolite['hmdb'] is not None):
    fh.write(metabolite['hmdb'])
  fh.write('\t')
  fh.write(metabolite['lmid'])
  fh.write('\t')
  if metabolite['kegg'] is not None:
    fh.write(metabolite['kegg'])
  fh.write('\t')
  if metabolite['chebi'] is not None:
    fh.write(metabolite['chebi'])
  fh.write('\t')
  if metabolite['pubchem_compound'] is not None:
    fh.write(metabolite['pubchem_compound'])
  fh.write('\n')

if __name__ == "__main__":
  main(sys.argv[1:])