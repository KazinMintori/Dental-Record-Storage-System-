package com.dentalclinic.service;

import com.dentalclinic.model.Patient;
import com.dentalclinic.model.PatientPage;
import com.dentalclinic.model.PatientSearchCriteria;
import com.dentalclinic.model.PatientGender;
import com.dentalclinic.repository.PatientRepository;
import com.dentalclinic.repository.RepositoryException;

import java.util.List;
import java.util.Objects;

public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = Objects.requireNonNull(patientRepository, "patientRepository must not be null");
    }

    public Patient createPatient(Patient patient) {
        validatePatient(patient);
        try {
            return patientRepository.save(patient);
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to save patient.", exception);
        }
    }

    public Patient getPatient(Long id) {
        requireId(id, "Patient ID is required.");
        try {
            return patientRepository.findById(id);
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to load patient.", exception);
        }
    }

    public List<Patient> getAllPatients() {
        try {
            return patientRepository.findAll();
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to load patients.", exception);
        }
    }

    public List<Patient> searchPatients(String name) {
        return searchPatients(new PatientSearchCriteria(name, null, null, null, null));
    }

    public List<Patient> searchPatients(PatientSearchCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria must not be null");
        try {
            return patientRepository.search(criteria);
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to search patients.", exception);
        }
    }

    public PatientPage searchPatientPage(PatientSearchCriteria criteria, int pageIndex, int pageSize) {
        Objects.requireNonNull(criteria, "criteria must not be null");
        try {
            return patientRepository.searchPage(criteria, pageIndex, pageSize);
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to load patient page.", exception);
        }
    }

    public void updatePatient(Patient patient) {
        validatePatient(patient);
        try {
            patientRepository.update(patient);
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to update patient.", exception);
        }
    }

    public void deletePatient(Long id) {
        requireId(id, "Patient ID is required.");
        try {
            patientRepository.delete(id);
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to delete patient.", exception);
        }
    }

    public List<Patient> getDeletedPatients() {
        try {
            return patientRepository.findDeleted();
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to load deleted patients.", exception);
        }
    }

    public PatientPage getDeletedPatientPage(int pageIndex, int pageSize) {
        try {
            return patientRepository.findDeletedPage(pageIndex, pageSize);
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to load deleted-patient page.", exception);
        }
    }

    public void deletePatients(List<Long> ids) {
        requireIds(ids);
        try {
            patientRepository.deleteAll(ids);
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to delete patients.", exception);
        }
    }

    public void restorePatients(List<Long> ids) {
        requireIds(ids);
        try {
            patientRepository.restoreAll(ids);
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to restore patients.", exception);
        }
    }

    public void permanentlyDeletePatients(List<Long> ids) {
        requireIds(ids);
        try {
            patientRepository.permanentlyDeleteAll(ids);
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to permanently delete patients.", exception);
        }
    }

    public void restorePatient(Long id) {
        requireId(id, "Patient ID is required.");
        try {
            patientRepository.restore(id);
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to restore patient.", exception);
        }
    }

    public void permanentlyDeletePatient(Long id) {
        requireId(id, "Patient ID is required.");
        try {
            patientRepository.permanentlyDelete(id);
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to permanently delete patient.", exception);
        }
    }

    private static void validatePatient(Patient patient) {
        if (patient == null) {
            throw new ServiceException("Patient is required.");
        }
        requireText(patient.getHoVaTen(), "Patient name is required.");
        requireText(patient.getGioiTinh(), "Patient gender is required.");
        if (!PatientGender.isSupported(patient.getGioiTinh())) {
            throw new ServiceException("Patient gender must be Nam, Nữ, or Khác.");
        }
        if (patient.getNgaySinh() == null) {
            throw new ServiceException("Patient birth date is required.");
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ServiceException(message);
        }
    }

    private static void requireId(Long id, String message) {
        if (id == null) {
            throw new ServiceException(message);
        }
    }

    private static void requireIds(List<Long> ids) {
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(Objects::isNull)) {
            throw new ServiceException("At least one patient ID is required.");
        }
    }
}
