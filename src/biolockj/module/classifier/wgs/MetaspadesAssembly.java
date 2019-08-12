/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 9, 2017
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
public class MetaspadesAssembly extends ClassifierModuleImpl {
	/**
	 * Build bash script lines to assemble paired WGS reads with MetaSPAdes. The inner list contains 1 bash script line
	 * per sample.
	 * <p>
	 * Example line:<br>
	 * python /app/metaspades.py -t 10 -m 150 -s single_end.fasta --tmp-dir ./temp/ -o ./output/
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception {
		final List<List<String>> data = new ArrayList<>();
		for( final File file: files ) {
			final String fileId = SeqUtil.getSampleId( file.getName() );
			final String outputFile = getOutputDir().getAbsolutePath() + File.separator + fileId +"_assembly."+ Constants.FASTA;
			final ArrayList<String> lines = new ArrayList<>();
			lines.add(FUNCTION_METASPADES + " " + file.getAbsolutePath() + " " + outputFile );
			data.add( lines );
		}

		return data;
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
			final String outputFile = getOutputDir().getAbsolutePath() + File.separator + fileId +"_assembly."+ Constants.FASTA;
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
	 */
	@Override
	public String getClassifierExe() throws Exception {
		return Config.getExe( this, EXE_METASPADES );
	}

	/**
	 * Obtain the metaspades runtime params
	 */
	@Override
	public List<String> getClassifierParams() throws Exception {
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
			lines.add(getClassifierExe() + " -t " + Config.getNonNegativeInteger(this, "script.numThreads") + " -m " + getMemory() + " -1 $1 -2 $2 --tmp-dir " + getTempDir().getAbsolutePath() + " -o $3" );
		}else {
			lines.add(getClassifierExe() + " -t " + Config.getNonNegativeInteger(this, "script.numThreads") + " -m " + getMemory() + " -s $1 --tmp-dir " + getTempDir().getAbsolutePath() + " -o $2" );
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
//	protected String getParams() throws Exception {
////		if( this.defaultSwitches == null ) {
////			final String params = BioLockJUtil.join( getClassifierParams() );
////
//////			if( params.indexOf( "--input_type " ) > -1 )
//////				throw new Exception( "Invalid classifier option (--input_type) found in property("
//////					+ EXE_METASPADES_PARAMS + "). BioLockJ derives this value by examinging one of the input files." );
//////			if( params.indexOf( NUM_THREADS_PARAM ) > -1 )
//////				throw new Exception( "Ignoring nvalid classifier option (" + NUM_THREADS_PARAM + ") found in property("
//////					+ EXE_METASPADES_PARAMS + "). BioLockJ derives this value from property: " + SCRIPT_NUM_THREADS );
//////			if( params.indexOf( "--bowtie2out " ) > -1 )
//////				throw new Exception( "Invalid classifier option (--bowtie2out) found in property("
//////					+ EXE_METASPADES_PARAMS + "). BioLockJ outputs bowtie2out files to Metaphlan2Classifier/temp." );
//////			if( params.indexOf( "-t rel_ab_w_read_stats " ) > -1 )
//////				throw new Exception( "Invalid classifier option (-t rel_ab_w_read_stats). BioLockJ hard codes this "
//////					+ "option for MetaPhlAn so must not be included in the property file." );
//////			if( params.indexOf( "--tax_lev " ) > -1 )
//////				throw new Exception( "Invalid classifier option (--tax_lev) found in property(" + EXE_METASPADES_PARAMS
//////					+ "). BioLockJ sets this value based on: " + Constants.REPORT_TAXONOMY_LEVELS );
//////			if( params.indexOf( "-s " ) > -1 ) throw new Exception( "Invalid classifier option (-s) found in property("
//////				+ EXE_METASPADES_PARAMS + "). SAM output not supported.  BioLockJ outputs " + TSV_EXT + " files." );
//////			if( params.indexOf( "-o " ) > -1 )
//////				throw new Exception( "Invalid classifier option (-o) found in property(" + EXE_METASPADES_PARAMS
//////					+ "). BioLockJ outputs results to: " + getOutputDir().getAbsolutePath() + File.separator );
//////
//////			this.defaultSwitches = getRuntimeParams( getClassifierParams(), NUM_THREADS_PARAM )
//////				+ "-t rel_ab_w_read_stats ";
////
////			if( TaxaUtil.getTaxaLevels().size() == 1 )
////				this.defaultSwitches += "--tax_lev " + this.taxaLevelMap.get( TaxaUtil.getTaxaLevels().get( 0 ) ) + " ";
////		}
////
////		return this.defaultSwitches;
//	}
	
//
//	private String getWorkerFunctionParams() throws Exception {
////		return " " + getParams() + INPUT_TYPE_PARAM + SeqUtil.getSeqType() + " ";
//	}
//
	private String defaultSwitches = null;

	/**
	 * {@link biolockj.Config} exe property used to obtain the metaphlan2 executable
	 */
	protected static final String EXE_METASPADES = "exe.metaspades";

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

	/**
	 * {@link biolockj.Config} Directory property containing alternate database: {@value #METASPADES_DB}<br>
	 * Must always be paired with {@value #METASPADES_MPA_PKL}
	 */
}
