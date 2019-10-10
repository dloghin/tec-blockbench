import sys                                                                      
import re                                                                       
                                                                                
clients = [3, 4, 5, 6, 7, 8, 9, 10,                                             
           11, 12, 13, 14, 16, 17, 18, 19,                                         
           20, 21, 22, 23, 24, 25, 26, 27,                                         
           28, 29, 30, 31, 32, 33, 34, 35]                                         
                                                                                
if len(sys.argv) != 4:                                                          
    print "python parse-result.py <NServer> <NThread> <TXRATE>"                    
                                                                                
N = sys.argv[1]                                                                 
T = sys.argv[2]                                                                 
R = sys.argv[3]                                                                 
                                                                                
lcnt = 0                                                                        
ltotal = 0.0                                                                    
count = 0                                                                       
notx = 0                                                                        
maxtps = 0.0                                                                    
                                                                                
for c in range(int(N)):                                                         
  filename = "logs/exp_{}_servers_{}_threads_{}_rates/client_10.0.0.{}_{}".format(N, T, R, clients[c], T)
  with open(filename, "r") as fp:                                               
    for line in fp:                                                             
      res = re.search('tx count = ([0-9]*) latency = ([0-9]*)', line, re.IGNORECASE)
      if res:                                                                   
        totallat = float(res.group(2))                                          
        txcnt = int(res.group(1))                                               
        if txcnt > 0:                                                           
          ltotal += totallat                                                    
          lcnt += txcnt                                                         
        count += 1                                                              
      elif line.startswith("polled block"):                                        
        idx = line.index(":")                                                   
        notx += int(line[idx+2:-6])                                             
  if count > 0 :                                                                
    tps = float(notx)/(count*10)                                                 
    if tps > maxtps:                                                            
      maxtps = tps                                                              
  notx = 0                                                                      
  count = 0                                                                     
                                                                                
latency = 0                                                                     
if lcnt > 0:                                                                    
  latency = ltotal/lcnt                                                         
print '{}\n{}'.format(maxtps, latency)
