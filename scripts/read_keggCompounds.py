#!/usr/bin/python
import sys
import getopt
import os.path

def main(argv):
  
  inputfile = ''
  outputfile = ''
  try:
    opts, args = getopt.getopt(argv,"h:i:o:k:",["ifile=","ofile=","kfile="])
  except getopt.GetoptError:
    print('extract_compounds.py -i <inputfile> -k <inchikeyfile> -o <outputfile>')
    sys.exit(2)
  for opt, arg in opts:
    if opt == '-h':
      print('extract_compounds.py -i <inputfile> -k <inchikeyfile> -o <outputfile>')
      sys.exit()
    elif opt in ("-i", "--ifile"):
      inputfile = arg
    elif opt in ("-o", "--ofile"):
      outputfile = arg
    elif opt in ("-k", "--kfile"):
      keyfile = arg
  
  print('Compound input file is',inputfile,)
  print('Table output file is',outputfile,)
  print('Key file is',keyfile,)
  
  if not os.path.exists(inputfile):
    print('Compound input file does not exist!')
  if not os.path.exists(keyfile):
    print('Compound input file does not exist!')
    
  print("Reading compounds")
  compounds = parse_input(inputfile)
  print("Read",len(compounds),"compounds")
  
  print("Reading inchi keys")
  inchiMap = parse_keyfile(keyfile)
  print("Read",len(inchiMap)," keys")
  
  write_file(outputfile,compounds,inchiMap)
  

def write_file(outputfile,compounds,inchiMap):
  f = open(outputfile,mode='w')
  
  f.write('KeggCompound\tInChI\tCAS\tPubChemSubstance\tChEBI\n')
  
  for c in compounds:
    f.write(c['entry'])
    f.write('\t')
    if c['entry'] in inchiMap:
      f.write(inchiMap[c['entry']])
    f.write('\t')
    if 'cas' in c:
      f.write(c['cas'])
    f.write('\t')
    if 'pubchem' in c:
      f.write(c['pubchem'])
    f.write('\t')
    if 'chebi' in c:
      f.write(c ['chebi'])
    f.write('\n')
  
  
  f.flush()
  f.close();

def parse_input(inputfile):
  f = open(inputfile,mode='r')
  
  lines = f.readlines()
  
  compounds = []
  
  i=0
  while i<len(lines):
    line = lines[i]
    if line.startswith("ENTRY"):
      entry,pos = parse_entry(lines,i)
      compounds.append(entry)
      i=pos
    i+=1
 
  f.close()
  return compounds
  
  
def parse_entry(lines,pos):
  #print ("Parsing: ",lines[pos].split())
  entry = dict()
  i=pos
  while i<len(lines):
    line = lines[i]
    if line.startswith("ENTRY"):
      entry['entry'] = line.split()[1]
    if line.startswith("DBLINKS"):
      line = line.replace("DBLINKS","")
      while line.startswith('\t') or line.startswith(' '):
        dblink = line.strip().split(':')
        #print("Reading entry",dblink)
        db = dblink[0].strip().lower()
        ident = dblink[1].strip()
        entry[db] = ident
        i+=1
        line = lines[i]
    if line.startswith("///"):
      break
    i+=1
  
  return entry,i

def parse_keyfile(keyfile):
  f = open(keyfile,mode='r')
  
  inchiMap = dict()
  
  for line in f:
    s = line.split()
    inchiMap[s[0].strip()]=s[1].strip()
  
  f.close()
  return inchiMap

if __name__ == "__main__":
  main(sys.argv[1:])