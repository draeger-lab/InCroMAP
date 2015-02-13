import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import de.zbit.gui.IntegratorUITools;
import de.zbit.io.OpenFile;
import de.zbit.io.ZIPUtils;
import de.zbit.mapper.enrichment.KeggID2PathwayMapper;
import de.zbit.util.Species;

/*
 * $Id:  KeggMappingsBinaryGenerator.java 13:13:29 rosenbaum $
 * $URL: KeggMappingsBinaryGenerator.java $
 * ---------------------------------------------------------------------
 * This file is part of Integrator, a program integratively analyze
 * heterogeneous microarray datasets. This includes enrichment-analysis,
 * pathway-based visualization as well as creating special tabular
 * views and many other features. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/InCroMAP> to
 * obtain the latest version of Integrator.
 *
 * Copyright (C) 2011-2015 by the University of Tuebingen, Germany.
 *
 * Integrator is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */

/**
 * This is just a small executable that will binarize the information
 * in the KEGG pathway files as they should not be part of the release
 * directly because of 
 * 
 * @author Lars Rosenbaum
 * @version $Rev$
 */

public class KeggMappingsBinaryGenerator {
	public static void main(String[] args) throws Exception{

		for(Species spec: IntegratorUITools.organisms){
			KeggID2PathwayMapper m = new KeggID2PathwayMapper(spec, null);
			
			BufferedReader input = OpenFile.openFile(m.getLocalFile());
			OutputStream cos = ZIPUtils.enCryptOutputStream(new FileOutputStream(m.getLocalFile()+".dat"));
			OutputStreamWriter w = new OutputStreamWriter(cos);
			
			while(input.ready()){
				w.write(input.readLine()+"\n");
			}
			w.flush();
			w.close();
		}
		
		for(Species spec: IntegratorUITools.organisms){
			KeggID2PathwayMapper m = new KeggID2PathwayMapper(spec, null);

			//InputStream cis = ZIPUtils.deCryptInputStream(new FileInputStream(m.getLocalFile()+".dat"));
			
			BufferedReader r = OpenFile.openFile(m.getLocalFile()+".dat", m.getClass(),true);
			//BufferedReader r = new BufferedReader(new InputStreamReader(cis));
			
			System.out.println(spec.getKeggAbbr());
			System.out.println(r.readLine());
		}
	}
		
}
