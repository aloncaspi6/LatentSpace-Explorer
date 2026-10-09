LatentSpace Explorer
A Java-based application for visually exploring word embeddings in latent space.

What is this?
Words can be represented as vectors in high-dimensional space, where similar words end up close to each other. This tool lets you interactively explore that space — search for words, measure semantic distances, and discover hidden relationships between concepts.
How it works
A Python script handles the heavy lifting: it loads a pre-trained embedding model and runs PCA to compress the vectors down to 50 dimensions. Java reads the output and handles everything else — the UI, the math, and the interactions.
Features

2D visualization with selectable PCA axes
Cosine similarity and Euclidean distance between words
K nearest neighbors for any word
Custom semantic axis projection (e.g. project all words onto a "rich ↔ poor" axis)
Vector arithmetic (king − man + woman ≈ queen)
Subspace grouping with centroid-based neighbor search

Running the app
Make sure Python is installed and run the embedder script first to generate the embeddings file, then launch the Java application.

