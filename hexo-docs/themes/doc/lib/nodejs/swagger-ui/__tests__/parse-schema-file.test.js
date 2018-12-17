'use strict';

const ParseSchemaFileError = require('../parse-schema-file-error.js');

describe('parse-schema-file', () => {

  const mockGetSwagger = jest.fn()
    .mockImplementationOnce(() => {
      return Promise.resolve({
        swagger: {
          swaggerJson: 'SWAGGER'
        }
      });
    })
    .mockImplementationOnce(() => {
      return Promise.reject(new ParseSchemaFileError({
        'message': 'There is an error reading the file.',
        'filePath': '/path/to/swagger/petstore.json',
        'referencePath': 'path/to/md/file'
      }));
    });

  jest.mock('../../swagger-store', () => {
    return () => ({
      getSwagger: mockGetSwagger
    });
  });

  const parseSchemaFile = require('../parse-schema-file');

  test('should parse the schema file', async () => {
    const swaggerFilePath = '/path/to/swagger/petstore.json';
    const mdFilePath = 'path/to/md/file';
    const expectedOutput = {
      'pageSource': mdFilePath,
      'swagger': 'SWAGGER'
    };
    const hexo = {};
    await expect(parseSchemaFile(swaggerFilePath, mdFilePath, hexo)).resolves.toEqual(expectedOutput);
    expect(mockGetSwagger).toBeCalled();
  });

  test('should catch file read error for file read issues', async () => {
    const swaggerFilePath = '/path/to/swagger/petstore.json';
    const mdFilePath = 'path/to/md/file';
    await expect(parseSchemaFile(swaggerFilePath, mdFilePath)).rejects.toBeInstanceOf(ParseSchemaFileError);
  });
});
