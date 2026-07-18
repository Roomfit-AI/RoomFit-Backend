package com.roomfit.room;

import com.roomfit.product.catalog.GeneratedFurnitureCatalog;

import java.util.List;
import java.util.Optional;

/**
 * Visual-footprint boundary policy shared by uploads, validation and placement candidates.
 */
public final class FurnitureBoundary {

    public static final double WALL_CLEARANCE_METERS = 0.02;
    public static final double DEFAULT_WALL_THICKNESS_METERS = 0.12;
    public static final double EPSILON = 1.0e-9;
    private static final double RIGHT_ANGLE_TOLERANCE_DEGREES = 1.0e-4;

    private FurnitureBoundary() {
    }

    public static Footprint footprint(Furniture furniture) {
        return footprint(furniture.getWidth(), furniture.getDepth(), furniture.getRotation(), furniture.getVariantId());
    }

    public static Footprint footprint(double width, double depth, double rotationDegrees) {
        return footprint(width, depth, rotationDegrees, null);
    }

    public static Footprint footprint(double width, double depth, double rotationDegrees, String variantId) {
        LocalFootprint local = resolveLocalFootprint(width, depth, variantId);
        double normalizedRotation = normalizeDegrees(rotationDegrees);
        double nearestRightAngle = Math.rint(normalizedRotation / 90.0) * 90.0;
        double radians = Math.toRadians(
                Math.abs(normalizedRotation - nearestRightAngle) <= RIGHT_ANGLE_TOLERANCE_DEGREES
                        ? nearestRightAngle
                        : normalizedRotation);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);

        List<Offset> corners = List.of(
                rotate(local.minX(), local.minZ(), cosine, sine),
                rotate(local.maxX(), local.minZ(), cosine, sine),
                rotate(local.maxX(), local.maxZ(), cosine, sine),
                rotate(local.minX(), local.maxZ(), cosine, sine)
        );
        double minX = corners.stream().mapToDouble(Offset::x).min().orElseThrow();
        double maxX = corners.stream().mapToDouble(Offset::x).max().orElseThrow();
        double minZ = corners.stream().mapToDouble(Offset::z).min().orElseThrow();
        double maxZ = corners.stream().mapToDouble(Offset::z).max().orElseThrow();
        return new Footprint(maxX - minX, maxZ - minZ, minX, maxX, minZ, maxZ, corners);
    }

    public static LocalFootprint resolveLocalFootprint(double width, double depth, String variantId) {
        if (variantId != null) {
            Optional<GeneratedFurnitureCatalog.VisualFootprint> generated =
                    GeneratedFurnitureCatalog.get().visualFootprint(variantId);
            if (generated.isPresent()) {
                GeneratedFurnitureCatalog.VisualFootprint footprint = generated.get();
                return new LocalFootprint(
                        footprint.minX(), footprint.maxX(), footprint.minZ(), footprint.maxZ(),
                        FootprintSource.VARIANT_VISUAL);
            }
        }

        // Null variants use the legacy renderer. Unknown non-null variants also
        // route to that renderer in Web, so both explicitly fall back to the
        // centered nominal dimensions rather than an invented safety margin.
        return new LocalFootprint(-width / 2.0, width / 2.0, -depth / 2.0, depth / 2.0,
                variantId == null ? FootprintSource.LEGACY_NOMINAL : FootprintSource.UNKNOWN_VARIANT_NOMINAL);
    }

    public static boolean isInside(Room room, Furniture furniture) {
        return isInside(room, furniture.getPosition(), footprint(furniture));
    }

    public static boolean isInside(Room room, Position center, Footprint footprint) {
        if (!finitePosition(center)) {
            return false;
        }
        Optional<UsableBounds> usable = usableBounds(room);
        if (usable.isEmpty()) {
            return false;
        }
        UsableBounds bounds = usable.get();
        return footprint.corners().stream().allMatch(corner -> {
            double x = center.getX() + corner.x();
            double z = center.getZ() + corner.z();
            return x >= bounds.minX() - EPSILON && x <= bounds.maxX() + EPSILON
                    && z >= bounds.minZ() - EPSILON && z <= bounds.maxZ() + EPSILON;
        });
    }

    public static Optional<Position> clamp(Room room, Position proposed, Furniture furniture) {
        return clamp(room, proposed, footprint(furniture));
    }

    public static Optional<Position> clamp(Room room, Position proposed, Footprint footprint) {
        if (!finitePosition(proposed)) {
            return Optional.empty();
        }
        Optional<UsableBounds> usable = usableBounds(room);
        if (usable.isEmpty()) {
            return Optional.empty();
        }
        UsableBounds bounds = usable.get();
        double minX = bounds.minX() - footprint.minX();
        double maxX = bounds.maxX() - footprint.maxX();
        double minZ = bounds.minZ() - footprint.minZ();
        double maxZ = bounds.maxZ() - footprint.maxZ();
        if (maxX < minX - EPSILON || maxZ < minZ - EPSILON) {
            return Optional.empty();
        }
        return Optional.of(new Position(
                clamp(proposed.getX(), minX, maxX),
                clamp(proposed.getZ(), minZ, maxZ)
        ));
    }

    public static Optional<UsableBounds> usableBounds(Room room) {
        if (!finiteRoom(room)) {
            return Optional.empty();
        }
        WallInsets insets = wallInteriorInsets(room);
        UsableBounds usable = new UsableBounds(
                insets.left() + WALL_CLEARANCE_METERS,
                room.getWidth() - insets.right() - WALL_CLEARANCE_METERS,
                insets.top() + WALL_CLEARANCE_METERS,
                room.getDepth() - insets.bottom() - WALL_CLEARANCE_METERS
        );
        return usable.maxX() >= usable.minX() && usable.maxZ() >= usable.minZ()
                ? Optional.of(usable)
                : Optional.empty();
    }

    private static WallInsets wallInteriorInsets(Room room) {
        List<Wall> walls = room.getWalls();
        if (walls == null || walls.isEmpty()) {
            double inset = DEFAULT_WALL_THICKNESS_METERS / 2.0;
            return new WallInsets(inset, inset, inset, inset);
        }

        double left = 0;
        double right = 0;
        double top = 0;
        double bottom = 0;
        for (Wall wall : walls) {
            if (wall == null || wall.getStart() == null || wall.getEnd() == null) {
                continue;
            }
            double dx = wall.getEnd().getX() - wall.getStart().getX();
            double dz = wall.getEnd().getZ() - wall.getStart().getZ();
            double centerX = (wall.getStart().getX() + wall.getEnd().getX()) / 2.0;
            double centerZ = (wall.getStart().getZ() + wall.getEnd().getZ()) / 2.0;
            double thickness = wall.getThickness() > 0 ? wall.getThickness() : DEFAULT_WALL_THICKNESS_METERS;
            double tolerance = Math.max(thickness, 0.25);
            if (Math.abs(dz) >= Math.abs(dx)) {
                if (Math.abs(centerX) <= tolerance) {
                    left = Math.max(left, Math.max(0, centerX + thickness / 2.0));
                } else if (Math.abs(centerX - room.getWidth()) <= tolerance) {
                    right = Math.max(right,
                            Math.max(0, room.getWidth() - centerX + thickness / 2.0));
                }
            } else if (Math.abs(centerZ) <= tolerance) {
                top = Math.max(top, Math.max(0, centerZ + thickness / 2.0));
            } else if (Math.abs(centerZ - room.getDepth()) <= tolerance) {
                bottom = Math.max(bottom,
                        Math.max(0, room.getDepth() - centerZ + thickness / 2.0));
            }
        }
        return new WallInsets(left, right, top, bottom);
    }

    private static Offset rotate(double x, double z, double cosine, double sine) {
        return new Offset(x * cosine - z * sine, x * sine + z * cosine);
    }

    private static double normalizeDegrees(double rotation) {
        double normalized = rotation % 360.0;
        return normalized < 0 ? normalized + 360.0 : normalized;
    }

    private static double clamp(double value, double min, double max) {
        if (Math.abs(max - min) <= EPSILON) {
            return (min + max) / 2.0;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static boolean finiteRoom(Room room) {
        return room != null && Double.isFinite(room.getWidth()) && Double.isFinite(room.getDepth())
                && room.getWidth() > 0 && room.getDepth() > 0;
    }

    private static boolean finitePosition(Position position) {
        return position != null && Double.isFinite(position.getX()) && Double.isFinite(position.getZ());
    }

    public enum FootprintSource {
        VARIANT_VISUAL,
        LEGACY_NOMINAL,
        UNKNOWN_VARIANT_NOMINAL
    }

    public record LocalFootprint(
            double minX, double maxX, double minZ, double maxZ, FootprintSource source
    ) {
    }

    public record Offset(double x, double z) {
    }

    public record Footprint(
            double effectiveWidth,
            double effectiveDepth,
            double minX,
            double maxX,
            double minZ,
            double maxZ,
            List<Offset> corners
    ) {
        public Footprint {
            corners = List.copyOf(corners);
        }
    }

    public record UsableBounds(double minX, double maxX, double minZ, double maxZ) {
    }

    private record WallInsets(double left, double right, double top, double bottom) {
    }
}
