'use strict';

const mockFilterSecurity = jest.fn(swagger => swagger + '-SECURITY');
const mockFilterParameters = jest.fn(swagger => swagger + '-PARAMETERS');
const mockFilterPaths = jest.fn(swagger => swagger + '-PATHS');
const mockFilterOperations = jest.fn(swagger => swagger + '-OPERATIONS');

jest.mock('../filters', () => ({
  filterSecurity: mockFilterSecurity,
  filterParameters: mockFilterParameters,
  filterPaths: mockFilterPaths,
  filterOperations: mockFilterOperations
}));

describe('docExclude.index', () => {

  it('should apply the filters', () => {
    const docExclude = require('../index');

    const swagger = 'SWAGGER';

    const updatedSwagger = docExclude(swagger);

    const expectedSwagger = 'SWAGGER-SECURITY-PARAMETERS-PATHS-OPERATIONS';

    expect(mockFilterSecurity).toBeCalled();
    expect(mockFilterParameters).toBeCalled();
    expect(mockFilterPaths).toBeCalled();
    expect(mockFilterOperations).toBeCalled();

    expect(updatedSwagger).toBe(expectedSwagger);
  });
});
