package soot.jimple.infoflow.data.pathBuilders;

import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;
import soot.jimple.infoflow.sourcesSinks.manager.SourceInfo;

import java.util.HashSet;
import java.util.Set;

public class TaintlysisContextSensitivePathBuilder extends ContextSensitivePathBuilder {

    private Set<AbstractionAtSink> propagationReachedAbstractionsAtSink = new HashSet<>();

    /**
     * Creates a new instance of the {@link ContextSensitivePathBuilder} class
     *
     * @param manager  The data flow manager that gives access to the icfg and other
     *                 objects
     * @param executor
     */
    public TaintlysisContextSensitivePathBuilder(InfoflowManager manager, InterruptableExecutor executor) {
        super(manager, executor);
    }

    @Override
    public InfoflowResults getResults() {
        this.validateResults();
        return this.results;
    }

    private Set<Stmt> getPropagationReachedSinks() {
        Set<Stmt> propagationReachedSinks = new HashSet<>();

        for(AbstractionAtSink abstractionAtSink : this.propagationReachedAbstractionsAtSink) {
            propagationReachedSinks.add(abstractionAtSink.getSinkStmt());
        }

        return propagationReachedSinks;
    }

    private Set<Stmt> getResultReachedSinks() {
        Set<ResultSinkInfo> resultSinks = this.results.getResults().keySet();
        Set<Stmt> resultReachedSinks = new HashSet<>();

        for(ResultSinkInfo sinkInfo : resultSinks) {
            resultReachedSinks.add(sinkInfo.getStmt());
        }

        return resultReachedSinks;
    }

    private Set<ResultSinkInfo> getResultSinkInfo(Set<Stmt> propagationReachedSinks) {
        Set<ResultSinkInfo> res = new HashSet<>();
        Set<Stmt> addedInfo = new HashSet<>();

        for(AbstractionAtSink abstractionAtSink : this.propagationReachedAbstractionsAtSink) {
            Stmt stmt = abstractionAtSink.getSinkStmt();

            if(!propagationReachedSinks.contains(stmt)) {
                continue;
            }

            if(addedInfo.contains(stmt)) {
                continue;
            }

            ResultSinkInfo sinkInfo = new ResultSinkInfo(abstractionAtSink.getSinkDefinition(),
                    abstractionAtSink.getAbstraction().getAccessPath(), stmt);
            res.add(sinkInfo);
            addedInfo.add(stmt);
        }

        return res;
    }

    private void validateResults() {
        Set<Stmt> propagationReachedSinks = this.getPropagationReachedSinks();
        Set<Stmt> resultReachedSinks = this.getResultReachedSinks();
        propagationReachedSinks.removeAll(resultReachedSinks);
        this.logMissingSinks(propagationReachedSinks);
        this.addMissingSinks(propagationReachedSinks);
    }

    private ResultSourceInfo getSourceInfo() {
        Set<ResultSourceInfo> values = this.results.getResults().values();

        if(values.size() > 1) {
            throw new RuntimeException("We do not current support multiple sources at the same time");
        }

        return values.iterator().next();
    }

    private void addMissingSinks(Set<Stmt> propagationReachedSinks) {
        ResultSourceInfo sourceInfo = this.getSourceInfo();
        Set<ResultSinkInfo> sinkInfos = this.getResultSinkInfo(propagationReachedSinks);

        for(ResultSinkInfo sinkInfo : sinkInfos) {
            this.results.addResult(sinkInfo, sourceInfo);
        }
    }

    private void logMissingSinks(Set<Stmt> propagationReachedSinks) {
        Set<Stmt> loggedMissingSinks = new HashSet<>();

        for(AbstractionAtSink abstractionAtSink : this.propagationReachedAbstractionsAtSink) {
            Stmt stmt = abstractionAtSink.getSinkStmt();

            if(!propagationReachedSinks.contains(stmt)) {
                continue;
            }

            if(loggedMissingSinks.contains(stmt)) {
                continue;
            }


            ResultSinkInfo sinkInfo = new ResultSinkInfo(abstractionAtSink.getSinkDefinition(),
                    abstractionAtSink.getAbstraction().getAccessPath(), stmt);
            this.logger.info("Missing sink: " + sinkInfo);
            loggedMissingSinks.add(stmt);
        }
    }

    public void setPropagationResults(Set<AbstractionAtSink> res) {
        this.propagationReachedAbstractionsAtSink.addAll(res);
    }
}
