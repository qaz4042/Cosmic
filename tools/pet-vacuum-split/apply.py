#!/usr/bin/env python3
"""Patch a copy of deployed Character, preserving unrelated live extensions."""
from pathlib import Path
import sys
p=Path(sys.argv[1])/'client/Character.java'
s=p.read_text()
needle='    private volatile server.TrainingPetVacuum trainingPetVacuum;'
assert s.count(needle)==1 and 'setTrainingPetVacuumMask' not in s
s=s.replace(needle,'''    private volatile int trainingPetVacuumMask = 3;
    public int getTrainingPetVacuumMask() {
        return isTrainingPetVacuumActive() ? trainingPetVacuumMask : 0;
    }
    public synchronized boolean setTrainingPetVacuumMask(int mask) {
        if (mask < 0 || mask > 3) return false;
        if (mask != 0 && (!isAlive() || !isLoggedin() || isChangingMaps()
                || !server.TrainingAssist.isAllowed(getMap()) || server.TrainingPetVacuum.petIndex(this) < 0)) return false;
        // Keep the last filter for ordinary in-session map transfer restoration.
        if (mask != 0) trainingPetVacuumMask = mask;
        return setTrainingPetVacuum(mask != 0);
    }
'''+needle)
p.write_text(s)
