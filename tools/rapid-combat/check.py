from pathlib import Path
import xml.etree.ElementTree as ET
import subprocess, os
b=Path(__file__).resolve().parent
root=b.parents[1]
report=ET.parse(root/'target/surefire-reports/TEST-server.TrainingRapidCombatTest.xml')
cp=next(p.attrib['value'] for p in report.findall('./properties/property') if p.attrib['name']=='java.class.path')
cp=str(b/'Cosmic-rapid-test.jar')+os.pathsep+cp
java=Path('/Users/zubin/Library/Java/JavaVirtualMachines/temurin-21.0.12.1/Contents/Home/bin')
subprocess.run([str(java/'javac'),'-proc:none','-cp',cp,'-d',str(b/'checks'),str(b/'CheckRapidOverlay.java')],check=True)
subprocess.run([str(java/'java'),'-cp',str(b/'checks')+os.pathsep+cp,'CheckRapidOverlay'],cwd=root,check=True)
