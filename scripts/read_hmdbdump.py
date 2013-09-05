#!/usr/bin/python
import sys
import getopt
import os
import xml.etree.ElementTree as ET
import codecs

def main(argv):
  
  inputfolder = ''
  outprefix = ''

  try:
    opts, args = getopt.getopt(argv,"i:o:",["ifolder=","oprefix="])
  except getopt.GetoptError:
    print('read_hmdbdump.py -i <inputfolder> -o <outprefix>')
    sys.exit(2)
  for opt, arg in opts:
    if opt == '-h':
      print('read_hmdbdump.py -i <inputfolder> -o <outprefix>')
      sys.exit()
    elif opt in ("-i", "--ifile"):
      inputfolder = arg
    elif opt in ("-o", "--oprefix"):
      outprefix = arg
  
  print('Reading HMDB from folder',inputfolder)
  print('Writing to ',outprefix)
  
  if not os.path.exists(inputfolder):
    print('Compound input file does not exist!')
    sys.exit(2)
  
  
  files = os.listdir(inputfolder)
  print('Found',len(files),'xml files')
  
  o_second = codecs.open(outprefix + '.secondary_accesssions','w', 'utf-8')
  o_second.write('HMDB\tAlias1\tAlias2\t...\n')
  o_synonyms =  codecs.open(outprefix + '.synonyms','w', 'utf-8')
  o_synonyms.write('HMDB\tSynonym1\tSynonym2\t...\n')
  o_hmdb = codecs.open(outprefix+".txt",'w', 'utf-8')
  o_hmdb.write('InChIKey\tName\tHMDB\tLMID\tKegg\tCHEBI\tPC_compound\n')
  
  for f in files:
    print('Reading file',f)
    metabolite = parse_file(os.path.join(inputfolder,f))
    write_synonyms(o_synonyms, metabolite)
    write_secondary(o_second,metabolite)
    write_metab(o_hmdb,metabolite)
  
  o_second.flush()
  o_synonyms.flush()
  o_hmdb.flush()
  o_second.close()
  o_synonyms.close()
  o_hmdb.close()

def write_metab(fh,metabolite):
  if metabolite['inchikey'] is not None:
    fh.write(metabolite['inchikey'].strip())
  fh.write('\t')
  fh.write(metabolite['name'].strip())
  fh.write('\t')
  fh.write(metabolite['accession'].strip())
  fh.write('\t')
  fh.write('\t')
  if(metabolite['kegg'] is not None):
    fh.write(metabolite['kegg'].strip())
  fh.write('\t')
  if metabolite['chebi'] is not None:
    fh.write(metabolite['chebi'].strip())
  fh.write('\t')
  if metabolite['pubchem_compound'] is not None:
    fh.write(metabolite['pubchem_compound'].strip())
  fh.write('\n')
  

def write_synonyms(fh,metabolite):
  fh.write(metabolite['accession'])
  for s in metabolite['synonyms']:
    #s = s.encode(encoding='utf_8', errors='replace')
    fh.write('\t')
    fh.write(s)
  fh.write('\n')
  
def write_secondary(fh,metabolite):
  fh.write(metabolite['accession'])
  fh.write('\t')
  fh.write(metabolite['accession'])
  for s in metabolite['secondary_accessions']:
    fh.write('\t'+s)
  fh.write('\n')
  
  
def parse_file(f):
  
  secondary_accessions = []
  synonyms = []
  metabolite = dict()
  
  tree = ET.parse(f)
  root = tree.getroot()
  
  #get secondary accessions
  for child in root.findall('secondary_accessions'):
    for e in child:
      if e.tag == 'accession':
        secondary_accessions.append(e.text.strip())
  
  #get synonyms
  for child in root.findall('synonyms'):
    for e in child:
      if e.tag == 'synonym':
        synonyms.append(e.text.strip())
  
  #if(len(root.findall('name'))>0):
  metabolite['name']=root.findall('name')[0].text.strip()
  #elif(len(root.findall('common_name'))>0):
  #  metabolite['name']=root.findall('common_name')[0].text.strip()
  #else:
  #  print('Could not find name')
  #  sys.exit(2)
  #  metabolite['name']=''
    
  synonyms.append(metabolite['name'])
  metabolite['accession'] = root.findall('accession')[0].text
  metabolite['secondary_accessions'] = secondary_accessions
  metabolite['synonyms']=synonyms
  t = root.findall('inchi')[0].text
  if t is not None:
    t = t.replace('InChI=','')
  metabolite['inchi']=t
  t = root.findall('inchikey')[0].text
  if t is not None:
    t = t.replace('InChIKey=','')
  metabolite['inchikey']=t
  metabolite['cas']=root.findall('cas_registry_number')[0].text
  metabolite['chemspider']=root.findall('chemspider_id')[0].text
  metabolite['kegg']=root.findall('kegg_id')[0].text
  metabolite['chebi']=root.findall('chebi_id')[0].text
  metabolite['pubchem_compound']=root.findall('pubchem_compound_id')[0].text
  
  return metabolite
  

if __name__ == "__main__":
  main(sys.argv[1:])