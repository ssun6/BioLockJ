# Deployment path:  $DOCKER_DIR/GenomeAssembly.Dockerfile
# Use biolockj as a parent image
FROM biolockj/blj_basic_py2

#metaspades for assembling reads into contigs
ENV META_SPADE_URL="http://cab.spbu.ru/files/release3.13.0/SPAdes-3.13.0-Linux.tar.gz"
RUN cd /usr/local/bin && wget -qO- $META_SPADE_URL | bsdtar -xzf-
ENV PATH="/usr/local/bin/SPAdes-3.13.0-Linux:/usr/local/bin/SPAdes-3.13.0-Linux/bin:$PATH"
   
#metabat2 for binning contigs into bins
ENV META_BAT_URL="https://bitbucket.org/berkeleylab/metabat/downloads/metabat-static-binary-linux-x64_v2.12.1.tar.gz"
RUN cd /usr/local/bin && wget -qO- $META_BAT_URL | bsdtar -xzf- && \
	mv metabat matabat_temp && mv matabat_temp/* . && rm -rf matabat_temp
      
#checkM for checking quality and lineage of bins
RUN apt-get update && apt-get install -y gcc mono-mcs 
RUN apt-get install -y zlib1g-dev libbz2-dev liblzma-dev libncurses5-dev
RUN python -m pip install --upgrade pip && alias pip="/usr/local/bin/pip"
RUN pip install numpy && \
	pip install scipy && \
	pip install matplotlib==1.5.3 && \
	pip install setuptools && \
	pip install Cython && \
	pip install pysam && \
    pip install dendropy && \
    pip install checkm-genome

	
# Download the standard checkm DB
ENV CHECKM_DB_URL="https://data.ace.uq.edu.au/public/CheckM_databases/checkm_data_2015_01_16.tar.gz"
ENV CHECKM_DB_DIR="/usr/local/bin/checkm_data"
RUN mkdir ${CHECKM_DB_DIR} && cd ${CHECKM_DB_DIR} && \
	wget -qO- $CHECKM_DB_URL | bsdtar -xzf- && \
	echo ${CHECKM_DB_DIR} | checkm data setRoot ${CHECKM_DB_DIR}
	
#mash for calculating distances between genomes/bins
ENV MASH_URL="https://github.com/marbl/Mash/releases/download/v2.2/mash-Linux64-v2.2.tar"
RUN cd /usr/local/bin && wget -qO- $MASH_URL | bsdtar -xf- && \
	mv mash-Linux64-v2.2/* . && rm -rf mash-Linux64-v2.2
RUN chmod -R 777 /usr/local/bin

