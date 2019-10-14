import sys                                                                      
import re                                                                       
                                                                                
if len(sys.argv) != 2:                                                          
    print "python parse-result.py <file_path>"                                     
                                                                                
latency = []                                                                    
lcnt = 0;                                                                       
ltotal = 0;                                                                     
count = 0;                                                                      
notx = 0;                                                                       
                                                                                
with open(sys.argv[1], "r") as fp:                                              
    for line in fp:                                                             
        res = re.search('tx count = ([0-9]*) latency = ([0-9]*)', line, re.IGNORECASE)
        if res:                                                                 
            totallat = float(res.group(2))                                         
            txcnt = int(res.group(1))                                           
            if txcnt > 0:                                                       
                ltotal += totallat                                              
                lcnt += txcnt                                                   
                lat = totallat / txcnt                                          
                latency.append(float(lat))                                         
            count += 1                                                          
        elif line.startswith("polled block"):                                      
            idx = line.index(":")                                               
            #print line[idx+2:-6]                                               
            notx += int(line[idx+2:-6])
tps = 0
latency = 0
if lcnt > 0:
    latency = ltotal/lcnt
if count > 0:
    tps = float(notx)/(count*2)                                        
print '{}\n{}'.format(tps, latency)                          
#for l in latency:                                                              
    #print '{}'.format(l)                
