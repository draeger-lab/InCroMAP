# set path to flatfiles downloaded from KEGG FTP server
kegg.dir <- "/rascratch/user/eichner/kegg/KEGG_2013_07_29/"
output.dir <- "~/Desktop/"

# set paths and species
ko.list <- paste(kegg.dir, "ko/ko.list", sep="")
genes.pathway.list <- paste(kegg.dir, "links/genes_pathway.list", sep="")
genes.ko.list <- paste(kegg.dir, "links/genes_ko.list", sep="")
all.species <- c("hsa", "mmu", "rno")

# read generic ko.list file
ko.table <- as.matrix(read.table(ko.list, sep="\t"))
ko.table[,1] <- sub("path:ko", "", ko.table[,1])
ko.table[,3] <- sub("^ko:", "", ko.table[,3])

# read species-specific mapping files
genes.pathway.table <- as.matrix(read.table(genes.pathway.list, sep="\t"))
genes.ko.table <- as.matrix(read.table(genes.ko.list, sep="\t"))

for (species in all.species) {

  # extract relevant rows from ko.list file
  rel.gene.ids <- unique(grep(paste("^", species, ":", sep=""), genes.pathway.table[,1], value=TRUE))
  names(rel.gene.ids) <- genes.ko.table[match(rel.gene.ids, genes.ko.table[,1]),2]
  rel.path.ids <- sub(paste("^path:", species, sep=""), "", unique(grep(paste("^path:", species, sep=""), genes.pathway.table[,2], value=TRUE)))
  
  curr.ko.table <- ko.table[which(ko.table[,1] %in% rel.path.ids),]
  curr.ko.table[,1] <- paste("path:", species, curr.ko.table[,1], sep="")
  ko.gene.idx <- grep("^ko:", curr.ko.table[,2])
  curr.ko.table[ko.gene.idx,2] <- as.vector(rel.gene.ids[curr.ko.table[ko.gene.idx,2]])
  unmapped.idx <- which(is.na(curr.ko.table[,2]))
  if (length(unmapped.idx) > 0) {
    curr.ko.table <- curr.ko.table[-unmapped.idx,]
  }
  species.gene.idx <- grep(paste("^", species, ":", sep=""), curr.ko.table[,2])
  curr.ko.table[species.gene.idx,3] <- paste(species, ":", curr.ko.table[species.gene.idx,3], sep="")

  # write table with pathway to gene mapping
  write.table(curr.ko.table, file=paste(output.dir, species, ".list", sep=""), sep="\t", row.names=FALSE, col.names=FALSE, quote=FALSE)
}
