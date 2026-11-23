package server;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class TrainingMapTransferTest {
    private TrainingMapTransfer.Actor player(Object source) {
        var p = mock(TrainingMapTransfer.Actor.class);
        when(p.getTrainingMapKey()).thenReturn(source);
        when(p.isAlive()).thenReturn(true);
        when(p.isLoggedin()).thenReturn(true);
        when(p.isTrainingMapAllowed()).thenReturn(true);
        when(p.getTrainingAutoSkill()).thenReturn(-1);
        return p;
    }
    @Test void protectionPrecedesArrivalAndActivitiesResumeOnceAtNewAnchor() {
        Object from=new Object(), to=new Object();
        var p=player(from);
        when(p.isTrainingInvulnerable()).thenReturn(true);
        when(p.isTrainingSilenceEnabled()).thenReturn(true);
        when(p.isTrainingVacuumActive()).thenReturn(true);
        when(p.isTrainingPetVacuumActive()).thenReturn(true);
        when(p.getTrainingAutoSkill()).thenReturn(5001002);
        when(p.startTrainingVacuum()).thenReturn(true);
        when(p.setTrainingPetVacuum(true)).thenReturn(true);
        TrainingMapTransfer.prepare(p,to);
        TrainingMapTransfer.enter(p); // stale destination / old map
        verify(p,never()).setTrainingInvulnerable(true);
        when(p.getTrainingMapKey()).thenReturn(to);
        TrainingMapTransfer.enter(p);
        verify(p).setTrainingInvulnerable(true);
        verify(p).setTrainingSilenceEnabled(true);
        verify(p,never()).startTrainingVacuum();
        TrainingMapTransfer.complete(p);
        TrainingMapTransfer.complete(p);
        verify(p).startTrainingVacuum();
        verify(p).setTrainingPetVacuum(true);
        verify(p).setTrainingAutoSkill(5001002);
    }
    @Test void restrictedMapsDeathLogoutAndExplicitStopDoNotReactivateAnything() {
        for(int mode=0;mode<4;mode++) {
            Object from=new Object(),to=new Object(); var p=player(from);
            when(p.isTrainingInvulnerable()).thenReturn(true);
            when(p.isTrainingVacuumActive()).thenReturn(true);
            TrainingMapTransfer.prepare(p,to);
            when(p.getTrainingMapKey()).thenReturn(to);
            if(mode==0) when(p.isTrainingMapAllowed()).thenReturn(false);
            if(mode==1) when(p.isAlive()).thenReturn(false);
            if(mode==2) when(p.isLoggedin()).thenReturn(false);
            if(mode==3) TrainingMapTransfer.cancel(p);
            TrainingMapTransfer.enter(p); TrainingMapTransfer.complete(p);
            verify(p,never()).setTrainingInvulnerable(true);
            verify(p,never()).startTrainingVacuum();
        }
    }
    @Test void uncheckedSilenceDoesNotDisableDestinationRuleAndFailuresAreReported() {
        Object from=new Object(),to=new Object();var p=player(from);
        when(p.isTrainingVacuumActive()).thenReturn(true);
        when(p.isTrainingPetVacuumActive()).thenReturn(true);
        TrainingMapTransfer.prepare(p,to); when(p.getTrainingMapKey()).thenReturn(to);
        TrainingMapTransfer.enter(p); TrainingMapTransfer.complete(p);
        verify(p,never()).setTrainingSilenceEnabled(false);
        verify(p,times(2)).yellowMessage(anyString());
    }
    @Test void chainedWarpsKeepPendingActivitiesUntilFinalArrival() {
        Object from=new Object(),mid=new Object(),to=new Object();var p=player(from);
        when(p.isTrainingVacuumActive()).thenReturn(true);
        TrainingMapTransfer.prepare(p,mid);when(p.getTrainingMapKey()).thenReturn(mid);
        TrainingMapTransfer.enter(p);
        when(p.isTrainingVacuumActive()).thenReturn(false); // old task was cancelled
        TrainingMapTransfer.prepare(p,to);when(p.getTrainingMapKey()).thenReturn(to);
        TrainingMapTransfer.enter(p); TrainingMapTransfer.complete(p);
        verify(p).startTrainingVacuum();
    }
}
