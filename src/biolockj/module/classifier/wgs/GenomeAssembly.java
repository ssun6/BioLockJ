/**
 * @UNCC Fodor Lab
 * @author Shan Sun
 * @email ssun5@uncc.edu
 * @date Aug 12, 2019
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.classifier.wgs;

import java.io.File;
import java.util.*;
import biolockj.Config;
import biolockj.Constants;
import biolockj.exception.ConfigNotFoundException;
import biolockj.exception.ConfigPathException;
import biolockj.exception.ConfigViolationException;
import biolockj.module.classifier.ClassifierModuleImpl;
import biolockj.util.*;

/**
 * This BioModule builds the bash scripts used to execute metaspades.py to assemble WGS sequences with MetaSPAdes.
 * 
 * @blj.web_desc MetaSPAdes Classifier
 */
public class GenomeAssembly extends ClassifierModuleImpl {
	/**
	 * Build bash script lines to assemble paired WGS reads with MetaSPAdes. The inner list contains 1 bash script line
	 * per sample.
	 * <p>
	 * Example line:<br>
	 * python /app/metaspades.py -t 10 -m 150 -s single_end.fasta --tmp-dir ./temp/ -o ./output/
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception {
		return null;
	}

	/**
	 * Build bash script lines to assemble paired WGS reads with MetaSPAdes. 
	 * per sample.
	 * <p>
	 * Example line:<br>
	 * python /app/metaspades.py -t 10 -m 150 -1 R1.fasta -2 R2.fasta --tmp-dir ./temp/ -o ./output/
	 */
	@Override
	public List<List<String>> buildScriptForPairedReads( final List<File> files ) throws Exception {
		final List<List<String>> data = new ArrayList<>();
		final Map<File, File> map = SeqUtil.getPairedReads( files );
		for( final File file: map.keySet() ) {
			final String fileId = SeqUtil.getSampleId( file.getName() );
			final String outputFile = getOutputDir().getAbsolutePath() + File.separator + fileId +"_assembly";
			final ArrayList<String> lines = new ArrayList<>();
			lines.add( FUNCTION_METASPADES + " " + file.getAbsolutePath() + " " + map.get( file ).getAbsolutePath()
				+ " " + outputFile );
			data.add( lines );
		}

		return data;
	}

	/**
	 * Verify none of the derived command line parameters are included in
	 * {@link biolockj.Config}.{@value #EXE_METASPADES}{@value biolockj.Constants#PARAMS}
	 */
	@Override
	public void checkDependencies() throws Exception {
	}

	/**
	 * Metaspades runs python scripts, so no special command is required
	 * @throws ConfigViolationException 
	 */
	public String getClassifier1Exe() throws ConfigViolationException {
		return Config.getExe(this, EXE_METASPADES );
	}
	public String getClassifier2Exe() throws ConfigViolationException {
		return Config.getExe(this, EXE_METABAT2 );
	}
	public String getClassifier3Exe() throws ConfigViolationException {
		return Config.getExe(this, EXE_CHECKM );
	}

	/**
	 * Obtain the metaspades runtime params
	 */
	@Override
	public List<String> getClassifierParams() {
		final List<String> params = Config.getList( this, EXE_METASPADES_PARAMS );
		return params;
	}
	
	/**
	 * $1 - forward read $2 - reverse
	 */
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception {
		final List<String> lines = super.getWorkerScriptFunctions();
		lines.add( "function " + FUNCTION_METASPADES + "() {" );
		if (Config.getBoolean( this, Constants.INTERNAL_PAIRED_READS )) {
			lines.add(getClassifier1Exe() + " -t " + Config.getNonNegativeInteger(this, "script.numThreads") + " -m " + getMemory() + " -1 $1 -2 $2 --tmp-dir " + getTempDir().getAbsolutePath() + " -o $3/assembly" );
			lines.add(getClassifier2Exe() + " -v -m 2000  -i $3/assembly/contigs.fasta -o $3/bins/bin");
			lines.add(getClassifier3Exe() + " lineage_wf -f $3/CheckM.txt -x fa -t "+ Config.getNonNegativeInteger(this, "script.numThreads")+ " $3/bins/ $3/SCG ");
		}else {
			lines.add(getClassifierExe() + " -t " + Config.getNonNegativeInteger(this, "script.numThreads") + " s3/bins/ $3/SCG " + getMemory() + " -s $1 --tmp-dir " + getTempDir().getAbsolutePath() + " -o $2" );
		}		
		lines.add( "}" + RETURN );
		return lines;
	}
	//TODO get memory from config file
	public int getMemory(){
		return 150;
	}
	/**
	 * MetaSPAdes doesn't require parameters besides input files and output directory.
	 * Default threads is 16 and memory is 250G<br>
	 * Verify no invalid runtime params are passed and add rankSwitch if needed.<br>
	 * @return runtime parameters
	 * @throws Exception if errors occur
	 */

	/**
	 * {@link biolockj.Config} exe property used to obtain the metaphlan2 executable
	 */
	protected static final String EXE_METASPADES = "exe.metaspades";
	protected static final String EXE_METABAT2 = "exe.metabat2";
	protected static final String EXE_CHECKM = "exe.checkm";

	/**
	 * Name of the metaspades function used to assemble reads: {@value #FUNCTION_METASPADES}
	 */
	protected static final String FUNCTION_METASPADES = "runMetaspades";


	/**
	 * {@link biolockj.Config} List property used to obtain the metaphlan2 executable params
	 */
	protected static final String EXE_METASPADES_PARAMS = "exe.metaspadesParams";

	@Override
	public File getDB() throws ConfigPathException, ConfigNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getClassifierExe() throws ConfigViolationException {
		// TODO Auto-generated method stub
		return Config.getExe(this, EXE_METASPADES);
	}

	/**
	 * {@link biolockj.Config} Directory property containing alternate database: {@value #METASPADES_DB}<br>
	 * Must always be paired with {@value #METASPADES_MPA_PKL}
	 */
}
