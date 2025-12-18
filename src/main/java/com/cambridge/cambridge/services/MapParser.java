package com.cambridge.cambridge.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import de.topobyte.osm4j.core.access.OsmHandler;
import de.topobyte.osm4j.core.access.OsmInputException;
import de.topobyte.osm4j.core.model.iface.OsmBounds;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.pbf.seq.PbfReader;

@Component
public class MapParser implements OsmHandler {

    // Graph Data Structures
    // Map<NodeID, NodeObject>
    private final Map<Long, Node> nodes = new HashMap<>();
    // Adjacency List: Map<NodeID, List<Edge>>
    private final Map<Long, List<Edge>> adjacencyList = new HashMap<>();

    public Map<Long, Node> getNodes() {
        return nodes;
    }

    public Map<Long, List<Edge>> getAdjacencyList() {
        return adjacencyList;
    }

    // Find the nearest node ID to a given Lat/Lon
    public Long findNearestNode(double lat, double lon) {
        long nearestNodeId = -1;
        double minDistance = Double.MAX_VALUE;

        for (Node node : nodes.values()) {
            double distance = calculateDistance(
                new Node(0, lat, lon), // Temporary node for calculation
                node
            );

            if (distance < minDistance) {
                minDistance = distance;
                nearestNodeId = node.id;
            }
        }
        return nearestNodeId;
    }

    public void parse(String filePath) throws IOException, OsmInputException {
        System.out.println("Reading OSM data from: " + filePath);
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("File not found: " + filePath);
        }

        InputStream input = new FileInputStream(file);
        PbfReader reader = new PbfReader(input, true);
        reader.setHandler(this);
        reader.read();
        
        System.out.println("Finished parsing.");
        System.out.println("Nodes: " + nodes.size());
        System.out.println("Adjacency List Size: " + adjacencyList.size());
    }

    @Override
    public void handle(OsmBounds bounds) {
    }

    @Override
    public void handle(OsmNode node) {
        // Store node
        nodes.put(node.getId(), new Node(node.getId(), node.getLatitude(), node.getLongitude()));
    }

    @Override
    public void handle(OsmWay way) {
        // Build edges from way (no filtering - process all ways)
        int numberOfNodes = way.getNumberOfNodes();
        if (numberOfNodes < 2) return;

        for (int i = 0; i < numberOfNodes - 1; i++) {
            long fromId = way.getNodeId(i);
            long toId = way.getNodeId(i + 1);

            // Add edge from -> to
            Node fromNode = nodes.get(fromId);
            Node toNode = nodes.get(toId);
            
            if (fromNode != null && toNode != null) {
                double distance = calculateDistance(fromNode, toNode);
                adjacencyList.computeIfAbsent(fromId, k -> new ArrayList<>()).add(new Edge(toId, distance));
                adjacencyList.computeIfAbsent(toId, k -> new ArrayList<>()).add(new Edge(fromId, distance));
            }
        }
    }

    private double calculateDistance(Node node1, Node node2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(node2.lat - node1.lat);
        double lonDistance = Math.toRadians(node2.lon - node1.lon);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(node1.lat)) * Math.cos(Math.toRadians(node2.lat))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000; // convert to meters
    }

    @Override
    public void handle(OsmRelation relation) {
    }

    @Override
    public void complete() {
    }

    public static class Node {
        public long id;
        public double lat;
        public double lon;

        public Node(long id, double lat, double lon) {
            this.id = id;
            this.lat = lat;
            this.lon = lon;
        }
    }

    public static class Edge {
        public long targetNodeId;
        public double distance; // in meters

        public Edge(long targetNodeId, double distance) {
            this.targetNodeId = targetNodeId;
            this.distance = distance;
        }
    }
}
