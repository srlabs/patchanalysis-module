package de.srlabs.patchanalysis_module.analysis;

public class ProgressItem{
    private PatchanalysisService service;
    private String name = "";
    private double progress = 0;
    private double weight = 0;

    public ProgressItem(PatchanalysisService service, String name, double weight){
        this.service = service;
        this.name = name;
        this.weight = weight;
    }
    public void update(double progress, String statusMessage){
        this.progress = progress;
        if(service != null)
            service.updateProgress(statusMessage);
    }
    public double getWeight(){
        return weight;
    }
    public double getProgress(){
        return progress;
    }
    public String getName(){
        return name;
    }
}