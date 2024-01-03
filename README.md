Fingerprint Recognition Mobile App

Overview 

This Android application is designed to extract fingerprint images from a Suprema sensor and employ a Siamese model for fingerprint matching. The primary use case for this app is to ensure that individuals in Cameroon only log in once using the provided Suprema sensors.

Features

Fingerprint Extraction: Utilizes Suprema sensor to capture fingerprint images.
Siamese Model Matching: Implements a Siamese model to compare fingerprint images efficiently.
Triplet Loss for Embeddings: Utilizes triplet loss to generate 126-dimensional embeddings for each fingerprint image.
Dynamic Database Update: If a fingerprint is not matched, it is added to the database.
Threshold-based Verification: Compares the dot product of two fingerprint vectors; if it exceeds a threshold (e.g., 0.9), the fingerprints are considered a match.
Problem Statement
The conventional approach using a predefined matcher posed time complexity issues. To address this, a Siamese model was employed, and its encoder was utilized to extract embeddings for each fingerprint image. The implementation of triplet loss allowed efficient matching based on the generated embeddings.

Technology Stack

TensorFlow Lite: Used for deploying the Siamese model on Android devices.
Android Studio: The primary IDE for Android app development.
Suprema Sensor SDK: Integrated for capturing fingerprint images.
