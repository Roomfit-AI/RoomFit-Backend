package com.roomfit.placement;

import com.roomfit.product.domain.MockProduct;
import com.roomfit.room.Furniture;
import com.roomfit.room.Opening;
import com.roomfit.room.Position;
import com.roomfit.room.Room;

import java.util.ArrayList;
import java.util.List;

final class FeedbackPlacementCandidateGenerator {

    static final int MAX_POSITIONS_PER_PRODUCT = 8;
    private static final int MAX_SWAP_POSITIONS_PER_PRODUCT = 12;
    private static final double FURNITURE_GAP = 0.1;
    private static final double OPENING_CLEARANCE_DEPTH = 0.45;

    List<PlacementCandidate> forAdd(Room room, MockProduct product, FeedbackPlacement placement,
                                    Furniture reference) {
        List<PlacementCandidate> candidates = new ArrayList<>();
        int order = 0;
        for (double rotation : List.of(0.0, 90.0)) {
            FurnitureFootprint footprint = FurnitureFootprint.from(product.getWidth(), product.getDepth(), rotation);
            for (Position position : positionsForRelation(room, footprint, placement, reference)) {
                Position clamped = clamp(room, position, footprint);
                if (addUnique(candidates, clamped, rotation, order)) {
                    order++;
                }
                if (candidates.size() == MAX_POSITIONS_PER_PRODUCT) {
                    return List.copyOf(candidates);
                }
            }
        }
        return List.copyOf(candidates);
    }

    List<PlacementCandidate> forSwap(Room room, Furniture current, MockProduct product) {
        List<PlacementCandidate> candidates = new ArrayList<>();
        List<Double> rotations = uniqueRotations(current.getRotation());
        int order = 0;
        for (double rotation : rotations) {
            FurnitureFootprint footprint = FurnitureFootprint.from(product.getWidth(), product.getDepth(), rotation);
            Position clamped = clamp(room, current.getPosition(), footprint);
            if (addUnique(candidates, clamped, rotation, order)) {
                order++;
            }
        }
        for (double rotation : rotations) {
            FurnitureFootprint footprint = FurnitureFootprint.from(product.getWidth(), product.getDepth(), rotation);
            double x = current.getPosition().getX();
            double z = current.getPosition().getZ();
            List<Position> positions = List.of(
                    new Position(x - 0.25, z), new Position(x + 0.25, z),
                    new Position(x, z - 0.25), new Position(x, z + 0.25),
                    new Position(footprint.effectiveWidth() / 2.0, z),
                    new Position(room.getWidth() - footprint.effectiveWidth() / 2.0, z),
                    new Position(x, footprint.effectiveDepth() / 2.0),
                    new Position(x, room.getDepth() - footprint.effectiveDepth() / 2.0)
            );
            for (Position position : positions) {
                Position clamped = clamp(room, position, footprint);
                if (addUnique(candidates, clamped, rotation, order)) {
                    order++;
                }
                if (candidates.size() == MAX_SWAP_POSITIONS_PER_PRODUCT) {
                    return List.copyOf(candidates);
                }
            }
        }
        return List.copyOf(candidates);
    }

    private List<Position> positionsForRelation(Room room, FurnitureFootprint footprint,
                                                FeedbackPlacement placement, Furniture reference) {
        return switch (placement.relation()) {
            case NEXT_TO -> nextToPositions(footprint, reference, placement.side());
            case LEFT_OF -> List.of(relativePosition(footprint, reference, FeedbackSide.LEFT));
            case RIGHT_OF -> List.of(relativePosition(footprint, reference, FeedbackSide.RIGHT));
            case NEAR_WALL -> nearWallPositions(room, footprint);
            case NEAR_WINDOW -> nearWindowPositions(room, footprint);
            case IN_CORNER -> cornerPositions(room, footprint);
            case CENTER -> List.of(new Position(room.getWidth() / 2.0, room.getDepth() / 2.0));
            default -> throw new IllegalArgumentException("Unsupported ADD relation: " + placement.relation());
        };
    }

    private List<Position> nextToPositions(FurnitureFootprint footprint, Furniture reference, FeedbackSide preferredSide) {
        List<FeedbackSide> sides = switch (preferredSide) {
            case LEFT -> List.of(FeedbackSide.LEFT);
            case RIGHT -> List.of(FeedbackSide.RIGHT);
            case FRONT -> List.of(FeedbackSide.FRONT);
            case BACK -> List.of(FeedbackSide.BACK);
            case null -> List.of(FeedbackSide.RIGHT, FeedbackSide.LEFT, FeedbackSide.FRONT, FeedbackSide.BACK);
        };
        return sides.stream().map(side -> relativePosition(footprint, reference, side)).toList();
    }

    private Position relativePosition(FurnitureFootprint footprint, Furniture reference, FeedbackSide side) {
        FurnitureFootprint referenceFootprint = FurnitureFootprint.from(reference);
        double xDistance = referenceFootprint.effectiveWidth() / 2.0 + footprint.effectiveWidth() / 2.0 + FURNITURE_GAP;
        double zDistance = referenceFootprint.effectiveDepth() / 2.0 + footprint.effectiveDepth() / 2.0 + FURNITURE_GAP;
        return switch (side) {
            case LEFT -> new Position(reference.getPosition().getX() - xDistance, reference.getPosition().getZ());
            case RIGHT -> new Position(reference.getPosition().getX() + xDistance, reference.getPosition().getZ());
            case FRONT -> new Position(reference.getPosition().getX(), reference.getPosition().getZ() - zDistance);
            case BACK -> new Position(reference.getPosition().getX(), reference.getPosition().getZ() + zDistance);
        };
    }

    private List<Position> nearWallPositions(Room room, FurnitureFootprint footprint) {
        double halfWidth = footprint.effectiveWidth() / 2.0;
        double halfDepth = footprint.effectiveDepth() / 2.0;
        return List.of(
                new Position(halfWidth, room.getDepth() / 2.0),
                new Position(room.getWidth() - halfWidth, room.getDepth() / 2.0),
                new Position(room.getWidth() / 2.0, halfDepth),
                new Position(room.getWidth() / 2.0, room.getDepth() - halfDepth)
        );
    }

    private List<Position> nearWindowPositions(Room room, FurnitureFootprint footprint) {
        List<Position> positions = new ArrayList<>();
        for (Opening opening : room.getOpenings()) {
            if (!"window".equals(opening.getType())) continue;
            double center = opening.getOffset() + opening.getWidth() / 2.0;
            positions.add(switch (opening.getWall()) {
                case "north" -> new Position(center,
                        room.getDepth() - footprint.effectiveDepth() / 2.0 - OPENING_CLEARANCE_DEPTH);
                case "south" -> new Position(center,
                        footprint.effectiveDepth() / 2.0 + OPENING_CLEARANCE_DEPTH);
                case "east" -> new Position(
                        room.getWidth() - footprint.effectiveWidth() / 2.0 - OPENING_CLEARANCE_DEPTH, center);
                case "west" -> new Position(footprint.effectiveWidth() / 2.0 + OPENING_CLEARANCE_DEPTH, center);
                default -> new Position(room.getWidth() / 2.0, room.getDepth() / 2.0);
            });
        }
        return positions;
    }

    private List<Position> cornerPositions(Room room, FurnitureFootprint footprint) {
        double halfWidth = footprint.effectiveWidth() / 2.0;
        double halfDepth = footprint.effectiveDepth() / 2.0;
        return List.of(
                new Position(halfWidth, halfDepth),
                new Position(room.getWidth() - halfWidth, halfDepth),
                new Position(halfWidth, room.getDepth() - halfDepth),
                new Position(room.getWidth() - halfWidth, room.getDepth() - halfDepth)
        );
    }

    private List<Double> uniqueRotations(double currentRotation) {
        List<Double> rotations = new ArrayList<>();
        for (double rotation : List.of(currentRotation, currentRotation + 90.0, 0.0, 90.0, 180.0, 270.0)) {
            double normalized = FeedbackPlacementPolicy.normalize(rotation);
            if (rotations.stream().noneMatch(existing -> Math.abs(existing - normalized) < 1.0e-9)) {
                rotations.add(normalized);
            }
        }
        return rotations;
    }

    private Position clamp(Room room, Position position, FurnitureFootprint footprint) {
        double x = Math.max(footprint.effectiveWidth() / 2.0,
                Math.min(room.getWidth() - footprint.effectiveWidth() / 2.0, position.getX()));
        double z = Math.max(footprint.effectiveDepth() / 2.0,
                Math.min(room.getDepth() - footprint.effectiveDepth() / 2.0, position.getZ()));
        return new Position(x, z);
    }

    private boolean addUnique(List<PlacementCandidate> candidates, Position position, double rotation, int order) {
        boolean duplicate = candidates.stream().anyMatch(candidate ->
                Math.abs(candidate.position().getX() - position.getX()) < 1.0e-9
                        && Math.abs(candidate.position().getZ() - position.getZ()) < 1.0e-9
                        && Math.abs(candidate.rotation() - rotation) < 1.0e-9);
        if (duplicate) return false;
        candidates.add(new PlacementCandidate(position, rotation, order));
        return true;
    }

    record PlacementCandidate(Position position, double rotation, int order) {
    }
}
