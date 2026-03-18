package com.knowledgegraphx.dto;

import java.util.List;

public class GraphData {

    private List<GraphNode> nodes;
    private List<GraphLink> links;

    public GraphData() {}

    public GraphData(List<GraphNode> nodes, List<GraphLink> links) {
        this.nodes = nodes;
        this.links = links;
    }

    public static class GraphNode {
        private String id;
        private String label;
        private String type; // "document", "topic", "keyword"
        private int size;
        private String color;

        public GraphNode() {}

        public GraphNode(String id, String label, String type, int size, String color) {
            this.id = id;
            this.label = label;
            this.type = type;
            this.size = size;
            this.color = color;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
    }

    public static class GraphLink {
        private String source;
        private String target;
        private double weight;
        private String label;

        public GraphLink() {}

        public GraphLink(String source, String target, double weight, String label) {
            this.source = source;
            this.target = target;
            this.weight = weight;
            this.label = label;
        }

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }

        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }

        public double getWeight() { return weight; }
        public void setWeight(double weight) { this.weight = weight; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }

    public List<GraphNode> getNodes() { return nodes; }
    public void setNodes(List<GraphNode> nodes) { this.nodes = nodes; }

    public List<GraphLink> getLinks() { return links; }
    public void setLinks(List<GraphLink> links) { this.links = links; }
}
