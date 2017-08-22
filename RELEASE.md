Making a BeakerX release
===========================

This document guides a contributor through creating a release of BeakerX.

Install
-------

Review ``RAEDME.md`` and make sure that all dependencies are installed.

Clean the repository
--------------------

You can remove all non-tracked files with:

```bash
git clean -xfdi
```

This will ask you for confirmation before removing all untracked files. Make
sure the ``dist/`` folder is clean and avoid stale build from
previous attempts.

Create the release
------------------

We publish a Python source package and a Python universal binary wheel. We also publish a conda package on conda-forge (see below).
See the Python docs on [package uploading](https://packaging.python.org/guides/tool-recommendations/)
for twine setup instructions and for why twine is the recommended method.

Update the version in `beakerx/beakerx/_version.py` and commit the change and push the git tag.

```bash
cd beakerx
rm -rf dist
python setup.py sdist
python setup.py bdist_wheel --universal
twine upload dist/*
# get the sha256 hash for conda-forge install
shasum -a 256 dist/*.tar.gz
```

Publish on conda-forge
----------------------

To update the conda-feedstock repo:

- Fork https://github.com/conda-forge/beakerx-feedstock
- Update the `version` and `sha256` variable values in `recipe/meta.yaml`
- Commit changes and submit a PR for the new version

To build and upload a conda package:

```
# outside of beakerx conda environment
conda install conda-build
conda upgrade conda
conda upgrade conda-build
# inside of beakerx env and beakerx project root
conda build --python 3.5 PATH_TO_RECIPE
# conda-build will output something like `anaconda upload PATH_TO_beakerx-VERSION-py35HASH.tar.bz2`
conda install --use-local beakerx
conda convert --platform all PATH_TO_beakerx-VERSION-py35HASH.tar.bz2 -o beakerx/dist
anaconda upload beakerx/dist/beakerx-VERSION-py35HASH.tar.bz2
```

- Fork https://github.com/conda-forge/beakerx-feedstock
- Update the `version` and `sha256` variable values in `recipe/meta.yaml`
- Commit changes and submit a PR for the new version
