package com.dentalclinic.service;

import com.dentalclinic.model.Patient;
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
        try {
            return patientRepository.findByName(name);
        } catch (RepositoryException exception) {
            throw new ServiceException("Unable to search patients.", exception);
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

    private static void validatePatient(Patient patient) {
        if (patient == null) {
            throw new ServiceException("Patient is required.");
        }
        requireText(patient.getHoVaTen(), "Patient name is required.");
        requireText(patient.getGioiTinh(), "Patient gender is required.");
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
}
