#!/usr/bin/env python3
"""Capture real GLES/engine measurements on an explicitly selected debug device."""
import argparse, json, pathlib, re, statistics, subprocess, time
p=argparse.ArgumentParser()
p.add_argument('--adb',default='adb');p.add_argument('--serial',required=True)
p.add_argument('--output',type=pathlib.Path,required=True);p.add_argument('--seconds',type=float,default=12)
p.add_argument('--scenarios',nargs='+',default=['dense','multiball','particles','invaders','volley','tunnel'])
a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True)
def adb(*args): return subprocess.check_output([a.adb,'-s',a.serial,*args],text=True)
def quantile(values,q): return sorted(values)[min(len(values)-1,int((len(values)-1)*q))]
summary={'serial':a.serial,'timestamp':time.strftime('%Y-%m-%dT%H:%M:%S%z'),
         'api':adb('shell','getprop','ro.build.version.sdk').strip(),
         'device':adb('shell','getprop','ro.product.model').strip(),'scenarios':{}}
for name in a.scenarios:
    mode={'invaders':'INVADERS','volley':'VOLLEY','tunnel':'TUNNEL'}.get(name,'CLASSIC')
    adb('shell','am','force-stop','com.breakoutplus.debug');adb('logcat','-c')
    launch=adb('shell','am','start','-W','-n','com.breakoutplus.debug/com.breakoutplus.GameActivity',
               '--es','extra_mode',mode,'--ez','extra_debug_autoplay','true','--ez','extra_debug_perf','true',
               '--el','extra_debug_seed','20261005','--es','extra_debug_stress',name)
    if 'Status: ok' not in launch: raise RuntimeError(launch)
    time.sleep(a.seconds)
    raw=adb('logcat','-d','-s','BreakoutPerf:I','BreakoutAutoPlay:I','AndroidRuntime:E')
    (a.output/f'{name}.log').write_text(raw)
    if 'FATAL EXCEPTION' in raw or 'render_crash' in raw: raise RuntimeError(f'{name} crashed')
    records=[]
    for line in raw.splitlines():
        if 'cpu_ms=' in line:
            records.append({k:float(v) for k,v in re.findall(r'(cpu_ms|interval_ms|draws|primitives|vertices|objects|updates)=([\d.eE+-]+)',line)})
    records=records[120:] # omit startup/warmup, identically for before/after
    if len(records)<100: raise RuntimeError(f'{name}: only {len(records)} measured frames')
    metrics={'frames':len(records)}
    for key in ['cpu_ms','interval_ms','draws','primitives','vertices','objects','updates']:
        values=[r[key] for r in records]
        metrics[key]={'mean':statistics.mean(values),'p50':quantile(values,.5),'p95':quantile(values,.95),'p99':quantile(values,.99),'max':max(values)}
    metrics['interval_over_20ms']=sum(r['interval_ms']>20 for r in records)
    summary['scenarios'][name]=metrics
    (a.output/'summary.json').write_text(json.dumps(summary,indent=2))
    print(name,metrics['frames'],'frames','CPU p95',round(metrics['cpu_ms']['p95'],3),'draw mean',round(metrics['draws']['mean'],2),flush=True)
adb('shell','am','force-stop','com.breakoutplus.debug')
