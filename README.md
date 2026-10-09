# LatentSpace Explorer

A Java-based application for visualizing and exploring word embeddings in a multidimensional vector space.

The project uses Python to generate word embeddings and perform PCA for dimensionality reduction, while Java handles the visualization, vector operations, and user interactions.

## Features
- Interactive 2D and 3D visualization of word embeddings
- Cosine similarity and Euclidean distance calculations
- K-nearest neighbors search
- Vector arithmetic and centroid calculations
- Custom semantic axis projections
- Word search and highlighting
- Undo/Redo support

## Technologies
- Java and JavaFX
- Python (Gensim, NumPy, Scikit-learn)
- PCA (Principal Component Analysis)
- OOP principles
- Strategy and Command design patterns

## Running the Project

**Requirements:** Java, JavaFX, `json-simple`, and Python 3.

Install the Python dependencies:

```bash
python3 -m pip install gensim scikit-learn numpy
```

The application uses `embedder.py` to generate `full_vectors.json` and `pca_vectors.json` if they do not already exist.

Run `view.Main` in IntelliJ IDEA to start the application.

Make sure the required Python libraries are installed in the Python environment used by the application. The first run requires an internet connection to download the GloVe model.
