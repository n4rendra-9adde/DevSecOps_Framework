const app = require('../src/index');

describe('Health API', () => {
    test('GET /api/health returns UP', async () => {
        const supertest = require('supertest');
        const res = await supertest(app).get('/api/health');
        expect(res.statusCode).toBe(200);
        expect(res.body.status).toBe('UP');
    });

    test('GET /api/info returns correct language', async () => {
        const supertest = require('supertest');
        const res = await supertest(app).get('/api/info');
        expect(res.statusCode).toBe(200);
        expect(res.body.language).toBe('nodejs');
    });
});
