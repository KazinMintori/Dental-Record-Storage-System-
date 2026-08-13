package com.dentalclinic.service;

import com.dentalclinic.model.Revenue;
import com.dentalclinic.model.Visit;
import com.dentalclinic.repository.RepositoryException;
import com.dentalclinic.repository.RepositoryTransaction;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class VisitRevenueWorkflowService {

    private final RepositoryTransaction transaction;

    public VisitRevenueWorkflowService(RepositoryTransaction transaction) {
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
    }

    public VisitRevenueRecord create(Visit visit, List<Revenue> revenues) {
        requireInputs(visit, revenues);
        try {
            return transaction.execute(context -> {
                VisitService visitService = new VisitService(context.visitRepository());
                RevenueService revenueService = new RevenueService(context.revenueRepository());
                Visit savedVisit = visitService.createVisit(visit);
                List<Revenue> savedRevenues = revenues.stream()
                        .map(revenue -> attachAndCreate(revenueService, revenue, savedVisit.getId()))
                        .toList();
                return new VisitRevenueRecord(savedVisit, savedRevenues);
            });
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to save visit and revenue entries.", exception);
        }
    }

    public VisitRevenueRecord update(Visit visit, List<Revenue> revenues) {
        requireInputs(visit, revenues);
        if (visit.getId() == null) {
            throw new ServiceException("Visit ID is required for update.");
        }
        try {
            return transaction.execute(context -> {
                VisitService visitService = new VisitService(context.visitRepository());
                RevenueService revenueService = new RevenueService(context.revenueRepository());
                visitService.updateVisit(visit);

                List<Revenue> existing = revenueService.getVisitRevenue(visit.getId());
                Set<Long> retainedIds = new HashSet<>();
                for (Revenue revenue : revenues) {
                    revenue.setVisitId(visit.getId());
                    if (revenue.getId() == null) {
                        revenueService.createRevenue(revenue);
                    } else {
                        retainedIds.add(revenue.getId());
                        revenueService.updateRevenue(revenue);
                    }
                }
                existing.stream()
                        .map(Revenue::getId)
                        .filter(id -> !retainedIds.contains(id))
                        .forEach(revenueService::deleteRevenue);
                return new VisitRevenueRecord(visit, revenueService.getVisitRevenue(visit.getId()));
            });
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to update visit and revenue entries.", exception);
        }
    }

    public void delete(Long visitId) {
        try {
            transaction.execute(context -> {
                new VisitService(context.visitRepository()).deleteVisit(visitId);
                return null;
            });
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to delete visit.", exception);
        }
    }

    private static Revenue attachAndCreate(RevenueService service, Revenue revenue, Long visitId) {
        revenue.setVisitId(visitId);
        return service.createRevenue(revenue);
    }

    private static void requireInputs(Visit visit, List<Revenue> revenues) {
        if (visit == null) {
            throw new ServiceException("Visit is required.");
        }
        if (revenues == null) {
            throw new ServiceException("Revenue entries are required.");
        }
    }

    public record VisitRevenueRecord(Visit visit, List<Revenue> revenues) {
        public VisitRevenueRecord {
            revenues = List.copyOf(revenues);
        }
    }
}
