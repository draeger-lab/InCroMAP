package de.zbit.data.mRNA;

import de.zbit.data.Signal;
import de.zbit.data.id.GeneID;


/**
 * A class to hold {@link mRNA} probes with {@link Signal}s and {@link GeneID}s.
 * There are no new methods/fields in this class. With this class the toolbar can visualize specific Buttons for
 * time series data.
 * @author Felix Bartusch
 */
public class mRNATimeSeries extends mRNA {

	public mRNATimeSeries(String name) {
		super(name);
	}

	public mRNATimeSeries(String probeName, String geneName, int geneID) {
		super(probeName, geneName, geneID);
	}

	public mRNATimeSeries(String name, int geneID) {
		super(name, geneID);
	}
}
