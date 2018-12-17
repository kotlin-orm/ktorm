'use strict';


/**
 * Mocking crypto.createHash('md5').update('...').digest('hex')
 */
const mockCrypto = {
  createHash: jest.fn(() => {
    return mockCrypto;
  }),
  update: jest.fn(() => {
    return mockCrypto;
  }),
  digest: jest.fn(() => {
    return 'DIGEST';
  })
};

jest.mock('crypto',() =>  mockCrypto);

function mockSwagger (){
  this.swaggerObject = '';

  this.validate = () => {
    this.swaggerObject += '-VALIDATED';
    return Promise.resolve(this);
  };

  this.merge = () => {
    this.swaggerObject += '-MERGED';
    return Promise.resolve(this);
  };

  this.unmerge = () => {
    this.swaggerObject += '-UNMERGED';
    return Promise.resolve(this);
  };

  this.decorate = mockSwagger.prototype.decorate = (x) => {
    this.swaggerObject += '-' + x();
    return Promise.resolve(this);
  };
}

jest.mock('../swagger', () => ({
  Swagger:  mockSwagger,
  decorators: {
    docExclude: () => 'DOC_EXCLUDE' ,
    host: () => 'HOST'
  }
}));

describe('swagger-store', () => {
  const mockHexo = {
    config: {
      theme_config: {}
    }
  };
  const swaggerStore = require('../swagger-store')({hexo: mockHexo});

  it('should return swagger.helpers', () => {
    expect(Object.keys(swaggerStore).length).toBe(5);
  });

  it('should get digest', () => {
    const digest = swaggerStore.getDigest('/path/to/file');
    expect(digest).toEqual('DIGEST');
    expect(mockCrypto.createHash).toHaveBeenCalled();
    expect(mockCrypto.createHash).toHaveBeenCalledWith('md5');
    expect(mockCrypto.update).toHaveBeenCalled();
    expect(mockCrypto.update).toHaveBeenCalledWith('/path/to/file');
    expect(mockCrypto.digest).toHaveBeenCalled();
    expect(mockCrypto.digest).toHaveBeenCalledWith('hex');
  });

  it('should prepare routes', () => {
    const downloadRoute = swaggerStore.prepareRoute('/path/to/file.yaml');
    expect(downloadRoute).toBe('DIGEST/file.yaml');
  });

  it('should set and get route', () => {
    const result = swaggerStore.setRoute('ROUTE', 'DATA');
    const routeData = swaggerStore.getRoutes('ROUTE');
    expect(result).toBe(true);
    expect(routeData).toBe('DATA');
  });

  describe('when no route is passed in setRoute', () => {
    it('should no do anything', () => {
      const result = swaggerStore.setRoute();
      expect(result).toBe(false);
    });
  });

  describe('when no route is passed in getRoute', () => {
    it('should return all routes', () => {
      const routes = swaggerStore.getRoutes();
      expect(routes).toEqual({'ROUTE': 'DATA'});
    });
  });

  it('should get swagger', () => {
    return swaggerStore
      .getSwagger('/path/to/swagger')
      .then((swagger) => {
        expect(swagger.downloadRoute).toBe('DIGEST/swagger');
        expect(swagger.swagger.swaggerObject).toBe('-MERGED-DOC_EXCLUDE-HOST-UNMERGED');
      });
  });
});
