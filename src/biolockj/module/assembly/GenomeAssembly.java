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
package biolockj.module.assembly;

import java.io.File;
import java.util.*;
import biolockj.Config;
import biolockj.Constants;
import biolockj.exception.ConfigViolationException;
import biolockj.module.SeqModuleImpl;
import biolockj.util.*;

/**
 * This BioModule builds the bash scripts used to assemble WGS sequences with MetaSPAdes, 
 * bin contigs with Metabat2 and check quality with checkM.
 * @blj.web_desc
 */
public class GenomeAssembly extends SeqModuleImpl {
	/**
	 * Build bash script lines to assemble paired WGS reads per sample.
	 * <p>
	 * Example line:<br>
	 * metaspades.py -t 2 -m 150 -1 $1 -2 $2 --tmp-dir ${pipeDir}/02_GenomeAssembly/temp -o $3/assembly
     * metabat2 -v -m 2000  -i $3/assembly/contigs.fasta -o $3/bins/bin
     * checkm lineage_wf -f $3/CheckM.txt -x fa -t 2 $3/bins/ $3/SCG
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception {
		return null;
	}

	@Override
	public List<List<String>> buildScriptForPairedReads( final List<File> files ) throws Exception {
		final List<List<String>> data = new ArrayList<>();
		final Map<File, File> map = SeqUtil.getPairedReads( files );
		for( final File file: map.keySet() ) {
			final String fileId = SeqUtil.getSampleId( file.getName() );
			final String outputFile = getOutputDir().getAbsolutePath() + File.separator + fileId +"_assembly";
			final ArrayList<String> lines = new ArrayList<>();
			lines.add( FUNCTION_ASSEMBLY + " " + file.getAbsolutePath() + " " + map.get( file ).getAbsolutePath()
				+ " " + outputFile );
			data.add( lines );
		}

		return data;
	}

	/**
	 * make sure the samples are paired reads, metaspades currently doesn't support single reads.
	 */
	@Override
	public void checkDependencies() throws Exception {
		if (!Config.getBoolean( this, Constants.INTERNAL_PAIRED_READS )) {
			System.err.println("Metaspades doesn't support single reads.");
		}		
	}

	/**
	 * No special command is required
	 * @throws ConfigViolationException 
	 */
	public String getMetaspadesExe() throws ConfigViolationException {
		return Config.getExe(this, EXE_METASPADES );
	}
	public String getMetabatExe() throws ConfigViolationException {
		return Config.getExe(this, EXE_METABAT2 );
	}
	public String getCheckmExe() throws ConfigViolationException {
		return Config.getExe(this, EXE_CHECKM );
	}

	/**
	 * Obtain the runtime params
	 */
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
		if (Config.getBoolean( this, Constants.INTERNAL_PAIRED_READS )) {
			lines.add( "function " + FUNCTION_ASSEMBLY + "() {" );
			lines.add(getMetaspadesExe() + " -t " + Config.getNonNegativeInteger(this, "script.numThreads") + " -m " + getMemory() + " -1 $1 -2 $2 --tmp-dir " + getTempDir().getAbsolutePath() + " -o $3/assembly" );
			lines.add(getMetabatExe() + " -v -m 2000  -i $3/assembly/contigs.fasta -o $3/bins/bin");
			lines.add(getCheckmExe() + " lineage_wf -f $3/CheckM.txt -x fa -t "+ Config.getNonNegativeInteger(this, "script.numThreads")+ " $3/bins/ $3/SCG ");
			lines.add( "}" + RETURN );
		}else {
			System.err.println("Metaspades doesn't support single reads.");
	    }
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
	protected static final String EXE_METASPADES = "exe.metaspades.py";
	protected static final String EXE_METABAT2 = "exe.metabat2";
	protected static final String EXE_CHECKM = "exe.checkm";

	/**
	 * Name of the metaspades function used to assemble reads: {@value #FUNCTION_ASSEMBLY}
	 */
	protected static final String FUNCTION_ASSEMBLY = "runAssembly";


	/**
	 * {@link biolockj.Config} List property used to obtain the assembly executable params
	 */
	protected static final String EXE_METASPADES_PARAMS = "exe.metaspadesParams";

}
