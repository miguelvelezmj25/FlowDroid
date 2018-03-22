package edu.cmu.cs.mvelezce.soot.jimple.jtp;

import soot.*;
import soot.jimple.Jimple;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.tagkit.BytecodeOffsetTag;
import soot.tagkit.LineNumberTag;
import soot.tagkit.Tag;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class Nop extends BodyTransformer {
    private static Nop instance = new Nop();

    private Nop() {
        ;
    }

    public static Nop v() {
        return Nop.instance;
    }

    @Override
    protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
        PatchingChain<Unit> units = b.getUnits();
        Iterator<Unit> unitsIterator = b.getUnits().snapshotIterator();

        SootClass sootClass = Scene.v().loadClassAndSupport("edu.cmu.cs.mvelezce.analysis.option.Sink");
        SootMethod sootMethod = sootClass.getMethod("void init()");

        while (unitsIterator.hasNext()) {
            Unit unit = unitsIterator.next();

            Tag lineNumberTag = null;
            Tag bytecodeOffsetTag = null;

            for(Tag tag : unit.getTags()) {
                if(tag instanceof LineNumberTag) {
                    int javaLine = ((LineNumberTag) tag).getLineNumber();
                    lineNumberTag = new LineNumberTag(javaLine);
                }

                if(tag instanceof BytecodeOffsetTag) {
                    int bytecodeIndex = ((BytecodeOffsetTag) tag).getBytecodeOffset();
                    bytecodeOffsetTag = new BytecodeOffsetTag(bytecodeIndex);
                }
            }

            if(lineNumberTag == null) {
                lineNumberTag = new LineNumberTag(-1);
            }

            if(bytecodeOffsetTag == null) {
                bytecodeOffsetTag = new BytecodeOffsetTag(-1);
            }

            StaticInvokeExpr invExpr = Jimple.v().newStaticInvokeExpr(sootMethod.makeRef());
            Stmt nop = Jimple.v().newInvokeStmt(invExpr);

//            NopStmt nop = Jimple.v().newNopStmt();
            nop.addTag(new LineNumberTag(-1));
            nop.addTag(new BytecodeOffsetTag(-1));

            units.insertBefore(nop, unit);
        }
    }
}