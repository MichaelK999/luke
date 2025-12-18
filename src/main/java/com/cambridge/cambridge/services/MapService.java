package com.cambridge.cambridge.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.springframework.stereotype.Service;

@Service
public class MapService {

    private final MapParser mapParser;

    public MapService(MapParser mapParser) {
        this.mapParser = mapParser;
    }

    public List<double[]> calculateRoute(double originLat, double originLng, double destLat, double destLng) {
        // 1. Find the nearest OSM nodes to the start and end points
        Long startNodeId = mapParser.findNearestNode(originLat, originLng);
        Long endNodeId = mapParser.findNearestNode(destLat, destLng);

        if (startNodeId == -1 || endNodeId == -1) {
            throw new RuntimeException("Could not find nearest nodes in the map");
        }

        // Get the actual node objects
        MapParser.Node startNode = mapParser.getNodes().get(startNodeId);
        MapParser.Node endNode = mapParser.getNodes().get(endNodeId);

        System.out.println("=== Route Calculation ===");
        System.out.println("--- Start Node ---");
        System.out.println("  Requested: Lat=" + originLat + ", Lng=" + originLng);
        System.out.println("  Found Node ID: " + startNodeId);
        System.out.println("  Node Location: Lat=" + startNode.lat + ", Lng=" + startNode.lon);
        
        System.out.println("--- End Node ---");
        System.out.println("  Requested: Lat=" + destLat + ", Lng=" + destLng);
        System.out.println("  Found Node ID: " + endNodeId);
        System.out.println("  Node Location: Lat=" + endNode.lat + ", Lng=" + endNode.lon);

        // 2. Run Dijkstra
        System.out.println("--- Running Dijkstra ---");
        long startTime = System.currentTimeMillis();
        
        List<Long> path = dijkstra(startNodeId, endNodeId);
        
        long endTime = System.currentTimeMillis();
        System.out.println("Dijkstra completed in " + (endTime - startTime) + "ms");

        if (path.isEmpty()) {
            System.out.println("NO PATH FOUND between the two nodes!");
            return new ArrayList<>();
        }

        System.out.println("Path found!");
        System.out.println("  Number of nodes in path: " + path.size());

        // 3. Convert node IDs to coordinates
        List<double[]> coordinates = new ArrayList<>();
        for (Long nodeId : path) {
            MapParser.Node node = mapParser.getNodes().get(nodeId);
            if (node != null) {
                coordinates.add(new double[]{node.lat, node.lon});
            }
        }

        return coordinates;
    }
    
    // Dijkstra implementation
    private List<Long> dijkstra(Long startNodeId, Long endNodeId) {

        // Grab the adjacencyList from our map parser
        Map<Long, List<MapParser.Edge>> adjacencyList = mapParser.getAdjacencyList();

        Map<Long, Double> distances = new HashMap<>();
        Map<Long, Long> previous = new HashMap<>();

        // Priority queue stores [distance, nodeId]
        PriorityQueue<double[]> queue = new PriorityQueue<>((a, b) -> Double.compare(a[0], b[0]));

        distances.put(startNodeId, 0.0);
        queue.add(new double[]{0.0, startNodeId});

        while (!queue.isEmpty()) {
            double[] current = queue.poll();
            double currentDistance = current[0];
            long currentNodeId = (long) current[1];

            // reached destination
            if (currentNodeId == endNodeId) {
                return reconstructPath(previous, startNodeId, endNodeId);
            }

            // outdated entry
            if (currentDistance > distances.getOrDefault(currentNodeId, Double.MAX_VALUE)) {
                continue;
            }

            List<MapParser.Edge> neighbors = adjacencyList.get(currentNodeId);
            if (neighbors == null) continue;

            for (MapParser.Edge edge : neighbors) {
                double newDistance = currentDistance + edge.distance;

                if (newDistance < distances.getOrDefault(edge.targetNodeId, Double.MAX_VALUE)) {
                    distances.put(edge.targetNodeId, newDistance);
                    previous.put(edge.targetNodeId, currentNodeId);
                    queue.add(new double[]{newDistance, edge.targetNodeId});
                }
            }
        }

        return new ArrayList<>(); // no path found so empty array ):
    }

    private List<Long> reconstructPath(Map<Long, Long> previous, Long startNodeId, Long endNodeId) {
        List<Long> path = new ArrayList<>();
        Long current = endNodeId;

        while (current != null) {
            path.add(current);
            if (current.equals(startNodeId)) break;
            current = previous.get(current);
        }

        Collections.reverse(path);
        return path;
    }
}
