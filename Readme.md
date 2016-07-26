### Add musicg 
Source: [https://github.com/loisaidasam/musicg](https://github.com/loisaidasam/musicg), formerly [https://code.google.com/archive/p/musicg/](https://code.google.com/archive/p/musicg/)

	git clone https://github.com/loisaidasam/musicg.git

### Compile 

	sbt assembly

### Clear and create database (json file based)

	./clear-sb.sh


### Add fingerprint(s) to database

	./run.sh -f <list of PCM 16Bit 44100 stereo wav files>

### Find match

	./run.sh -r <list of CM 16Bit 44100 stereo wav file chunks>
	
