package com.hw2.book_recommender.service;

import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Service
public class RdfGraphService {

    public static class RdfEdge {
        private final String label;

        public RdfEdge(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final int W = 1600;
    private static final int H = 1200;
    private static final int PADDING = 140;
    private static final int NODE_RADIUS = 18;

    public String processAndVisualizeRdf(MultipartFile file) throws Exception {
        System.setProperty("java.awt.headless", "true");

        Model rdfModel = ModelFactory.createDefaultModel();
        rdfModel.read(file.getInputStream(), null, "RDF/XML");

        Graph<String, RdfEdge> graph = new DirectedSparseMultigraph<>();

        StmtIterator iter = rdfModel.listStatements();
        while (iter.hasNext()) {
            Statement stmt = iter.nextStatement();
            String subject = getShortName(stmt.getSubject().toString());
            String predicate = stmt.getPredicate().getLocalName();
            String object = getShortName(stmt.getObject().toString());

            graph.addVertex(subject);
            graph.addVertex(object);
            graph.addEdge(new RdfEdge(predicate), subject, object, EdgeType.DIRECTED);
        }

        FRLayout<String, RdfEdge> layout = new FRLayout<>(graph);
        layout.setSize(new Dimension(W, H));
        layout.setMaxIterations(700);
        layout.initialize();
        while (!layout.done()) {
            layout.step();
        }

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

        for (String v : graph.getVertices()) {
            Point2D p = layout.apply(v);
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
        }

        double rangeX = (maxX - minX == 0) ? 1 : maxX - minX;
        double rangeY = (maxY - minY == 0) ? 1 : maxY - minY;
        double scale = Math.min(
                (W - 2.0 * PADDING) / rangeX,
                (H - 2.0 * PADDING) / rangeY
        );

        for (String v : graph.getVertices()) {
            Point2D p = layout.apply(v);
            double nx = PADDING + (p.getX() - minX) * scale;
            double ny = PADDING + (p.getY() - minY) * scale;
            layout.setLocation(v, new Point2D.Double(nx, ny));
            layout.lock(v, true);
        }

        BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, W, H);

        g2d.setFont(new Font("SansSerif", Font.PLAIN, 11));
        for (RdfEdge edge : graph.getEdges()) {
            Point2D ps = layout.apply(graph.getSource(edge));
            Point2D pd = layout.apply(graph.getDest(edge));

            int x1 = (int) ps.getX(), y1 = (int) ps.getY();
            int x2 = (int) pd.getX(), y2 = (int) pd.getY();

            g2d.setColor(new Color(130, 130, 130));
            g2d.setStroke(new BasicStroke(1.3f));
            drawArrow(g2d, x1, y1, x2, y2);

            g2d.setColor(new Color(60, 60, 60));
            g2d.drawString(edge.toString(), (x1 + x2) / 2, (y1 + y2) / 2);
        }

        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        for (String v : graph.getVertices()) {
            Point2D p = layout.apply(v);
            int x = (int) p.getX();
            int y = (int) p.getY();

            g2d.setColor(new Color(220, 50, 50));
            g2d.fillOval(x - NODE_RADIUS, y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);

            g2d.setColor(new Color(140, 20, 20));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawOval(x - NODE_RADIUS, y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);

            g2d.setColor(Color.BLACK);
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(v, x - fm.stringWidth(v) / 2, y + NODE_RADIUS + 15);
        }

        g2d.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private void drawArrow(Graphics2D g2d, int x1, int y1, int x2, int y2) {
        double angle = Math.atan2(y2 - y1, x2 - x1);

        int sx = (int) (x1 + NODE_RADIUS * Math.cos(angle));
        int sy = (int) (y1 + NODE_RADIUS * Math.sin(angle));
        int ex = (int) (x2 - NODE_RADIUS * Math.cos(angle));
        int ey = (int) (y2 - NODE_RADIUS * Math.sin(angle));

        g2d.drawLine(sx, sy, ex, ey);

        int size = 9;
        int ax1 = (int) (ex - size * Math.cos(angle - Math.PI / 6));
        int ay1 = (int) (ey - size * Math.sin(angle - Math.PI / 6));
        int ax2 = (int) (ex - size * Math.cos(angle + Math.PI / 6));
        int ay2 = (int) (ey - size * Math.sin(angle + Math.PI / 6));
        g2d.fillPolygon(new int[]{ex, ax1, ax2}, new int[]{ey, ay1, ay2}, 3);
    }

    private String getShortName(String uri) {
        if (uri.contains("#")) return uri.substring(uri.lastIndexOf("#") + 1);
        if (uri.contains("/")) return uri.substring(uri.lastIndexOf("/") + 1);
        return uri;
    }
}