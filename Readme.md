### Add musicg

	git clone https://github.com/worldveil/dejavu.git

### Compile 

	sbt assembly

### Clear and create database (json file based)

	./clear-sb.sh


### Add fingerprint(s) to database

	./run.sh -f <list of PCM 16Bit 44100 stereo wav files>

### Find match

	./run.sh -r <list of CM 16Bit 44100 stereo wav file chunks>
	
