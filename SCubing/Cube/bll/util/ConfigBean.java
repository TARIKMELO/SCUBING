package bll.util;

public class ConfigBean {

	public ConfigBean(){}
	//Todas as propriedades devem ser strings
	public String numThreads;
	public String nomeLayer;
	public String circleRadius;
	
	
	
	
	public String getNomeLayer() {
		return nomeLayer;
	}

	public void setNomeLayer(String nomeLayer) {
		this.nomeLayer = nomeLayer;
	}

	public String getCircleRadius() {
		return circleRadius;
	}

	public void setCircleRadius(String circleRadius) {
		this.circleRadius = circleRadius;
	}
	public String geometryColumnName;

	public String getNumThreads() {
		return numThreads;
	}

	public String getGeometryColumnName() {
		return geometryColumnName;
	}

	public void setGeometryColumnName(String geometryColumnName) {
		this.geometryColumnName = geometryColumnName;
	}

	public void setNumThreads(String numThreads) {
		this.numThreads = numThreads;
	}
}
