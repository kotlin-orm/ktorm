'use strict';


const mockHighlight = jest.fn(() => 'CURL_STRING');

jest.mock('../../../helpers', () => ({
  highlight: mockHighlight
}));

const requestSampleController = require('../requestSample');

describe('controllers.requestSample', () => {
  it('should transform the context as expected', () => {
    const requestBody = {
      'data': {
        'type': 'Order',
        'id': '123'
      }
    };

    const ctx = {
      'request': {
        'header': [
          {
            'name': 'Authorization',
            'in': 'header',
            'description': 'JWT token',
            'required': true,
            'type': 'string',
            'format': 'JWT token',
            'x-example': 'ThisIsAnAuthToken'
          },
          {
            'name': 'X-Flow-Id',
            'in': 'header',
            'description': 'For troubleshooting',
            'required': false,
            'type': 'string',
            'format': 'uuid',
            'x-example': 'ThisIsXFLOWID'
          }
        ],
        'path': [
          {
            'name': 'mid',
            'in': 'path',
            'description': 'To identify merchant this operation is carried out for',
            'required': true,
            'type': 'string',
            'format': 'uuid',
            'x-example': 'ThisIsMerchantId'
          },
          {
            'name': 'oid',
            'in': 'path',
            'description': 'The ID of the order for which the items are being requested.',
            'required': true,
            'type': 'string',
            'format': 'uuid',
            'x-example': 'ThisIsAnOrderId'
          }
        ],
        'query': [
          {
            'name': 'include',
            'in': 'query',
            'description': 'Comma seperated field names to be included',
            'required': false,
            'type': 'string',
            'x-example': 'ThisIsInclude'
          }
        ],
        'formData': [
          {
            'name': 'file',
            'in': 'formData',
            'description': "The merchant's document to create",
            'required': true,
            'type': 'file',
            'x-example': 'ThisIsInclude'
          },
          {
            'name': 'document-type',
            'in': 'formData',
            'description': "The merchant's document to create",
            'required': true,
            'type': 'string',
            'x-example': 'ThisIsInclude'
          }
        ],
        'body': [{
          'in': 'body',
          'name': 'order',
          'description': 'Patch of order',
          'required': true,
          'schema': {},
          'x-examples': {
            'default': requestBody
          }
        }]
      },
      'path': '/some/path/{mid}/{oid}',
      'verb': 'get',
      'baseUrl': 'https://example.com/'
    } ;

    let curlStr = 'curl -v -X GET https://example.com/some/path/ThisIsMerchantId/ThisIsAnOrderId?include=ThisIsInclude  \\\n-H "Authorization: Bearer <Access-Token>"  \\\n-H "X-Flow-Id: ThisIsXFLOWID"  \\\n-F "file: ThisIsInclude"  \\\n-F "document-type: ThisIsInclude"  \\\n';

    curlStr += JSON.stringify(requestBody, null, 2);

    const highlightArgs = {
      code: curlStr,
      lang: 'bash'
    };

    const {curlString} = requestSampleController(ctx);

    expect(mockHighlight).toHaveBeenCalled();

    expect(mockHighlight).toHaveBeenCalledWith(highlightArgs);

    expect(curlString).toBe('CURL_STRING');

  });
});

