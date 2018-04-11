package edu.cmu.cs.mvelezce.soot.jimple.jtp;

import soot.Body;
import soot.BodyTransformer;
import soot.Trap;
import soot.Unit;
import soot.jimple.internal.JGotoStmt;
import soot.jimple.internal.JRetStmt;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JReturnVoidStmt;
import soot.util.Chain;

import java.util.*;

public class IgnoreTraps extends BodyTransformer {
    private static IgnoreTraps instance = new IgnoreTraps();

    private IgnoreTraps() {
        ;
    }

    public static IgnoreTraps v() {
        return IgnoreTraps.instance;
    }

    @Override
    protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
        Chain<Trap> traps = b.getTraps();

        if(traps.isEmpty()) {
            return;
        }

        List<Unit> handlers = this.getHandlers(traps);
        Chain<Unit> units = b.getUnits();
        this.removeTraps(units, handlers);

        traps.clear();
    }

    private List<Unit> getHandlers(Chain<Trap> traps) {
        Set<Unit> uniqueHandlers = this.getUniqueHandlers(traps);
        List<Unit> handlers = new ArrayList<>();

        for(Trap trap : traps) {
            Unit handler = trap.getHandlerUnit();

            if(!uniqueHandlers.contains(handler)) {
                continue;
            }

            handlers.add(handler);
            uniqueHandlers.remove(handler);
        }

        return handlers;
    }

    private Set<Unit> getUniqueHandlers(Chain<Trap> traps) {
        Set<Unit> handlers = new HashSet<>();

        for(Trap trap : traps) {
            Unit handler = trap.getHandlerUnit();
            handlers.add(handler);
        }

        return handlers;
    }

    private void removeTraps(Chain<Unit> units, List<Unit> handlers) {
        for(Unit handler : handlers) {
            if(!units.contains(handler)) {
                continue;
            }

            Unit prev = units.getPredOf(handler);

            if(!(prev instanceof JGotoStmt) && !(prev instanceof JReturnStmt) && !(prev instanceof JReturnVoidStmt)) {
                throw new RuntimeException("The statement before the beginning of the handler is not a" +
                        " jump instruction");
            }

            if(prev instanceof JGotoStmt) {
                JGotoStmt gotoStmt = (JGotoStmt) prev;
                Unit target = gotoStmt.getTarget();
                Unit cur = handler;

                while(cur != target) {
                    Unit next = units.getSuccOf(cur);
                    units.remove(cur);
                    cur = next;
                }
            }
            else {
                Unit cur = handler;

                while(cur != null) {
                    Unit next = units.getSuccOf(cur);
                    units.remove(cur);
                    cur = next;
                }
            }

        }
    }
}