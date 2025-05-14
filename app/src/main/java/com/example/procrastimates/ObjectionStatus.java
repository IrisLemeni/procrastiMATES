package com.example.procrastimates;

// Enum for objection status
public enum ObjectionStatus {
    PENDING,     // Objection raised, waiting for proof
    RESOLVED,    // Proof provided and approved
    EXPIRED    // Objection denied or deadline passed
}
