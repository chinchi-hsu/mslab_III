To compile the code
===================
Just type:

    make


To run the code
===============

    java -cp bin:lib/opencsv-2.3.jar Evaluator LIBFMPATH INPUTFILE OUTPUTFILE NFACTORS USERCOL ITEMCOL

- **LIBFMPATH** the location of the libFM executable
- **INPUTFILE** the input CSV file
- **OUTPUTFILE** where to store the prediction result
- **NFACTORS** the more data, the higher is this value (set to 10 if in doubt)
- **USERCOL** the column of user ID in the input CSV file (start from 0)
- **ITEMCOL** the column of item ID in the input CSV file (start from 0)


Example
-------

    java -cp bin:lib/opencsv-2.3.jar Evaluator /root/libfm/libfm-1.42-src/bin/libFM input/product.csv output/result.csv 10 1 2

- libFM is located at /root/libfm/libfm-1.42-src/bin/libFM
- Reads input from input/product.csv
- The leave-one-out prediction results will be in the file output/result.csv
- Use 10 latent factors
- User IDs are in the column 1 of input/product.csv
- Item IDs are in the column 2 of input/product.csv
