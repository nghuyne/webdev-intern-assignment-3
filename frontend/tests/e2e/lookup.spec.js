import { test, expect } from '@playwright/test'

const API = 'http://localhost:8080'

test.describe('Tra cứu điểm thi (LookupPage)', () => {
  test('submit button is disabled until an SBD is entered', async ({ page }) => {
    await page.goto('/')
    const submit = page.getByRole('button', { name: 'Tra cứu' })
    await expect(submit).toBeDisabled()

    await page.getByPlaceholder('Nhập số báo danh (8 chữ số)').fill('12345678')
    await expect(submit).toBeEnabled()
  })

  test('valid SBD shows per-subject scores and foreign language', async ({ page }) => {
    await page.route(`${API}/api/scores/12345678`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          sbd: '12345678',
          maNgoaiNgu: 'N1',
          tenNgoaiNgu: 'Tiếng Anh',
          scores: [
            { maMon: 'toan', tenMon: 'Toán', diem: 8.75 },
            { maMon: 'ngu_van', tenMon: 'Ngữ văn', diem: 7.5 },
          ],
        }),
      })
    })

    await page.goto('/')
    await page.getByPlaceholder('Nhập số báo danh (8 chữ số)').fill('12345678')
    await page.getByRole('button', { name: 'Tra cứu' }).click()

    await expect(page.getByText('12345678')).toBeVisible()
    await expect(page.getByText('Tiếng Anh')).toBeVisible()
    await expect(page.getByText('Toán')).toBeVisible()
    await expect(page.getByText('8.75')).toBeVisible()
  })

  test('unknown SBD shows the 404 error message instead of results', async ({ page }) => {
    await page.route(`${API}/api/scores/99999999`, async (route) => {
      await route.fulfill({
        status: 404,
        contentType: 'application/json',
        body: JSON.stringify({
          timestamp: new Date().toISOString(),
          status: 404,
          error: 'Not Found',
          message: 'Không tìm thấy thí sinh với SBD 99999999',
          path: '/api/scores/99999999',
        }),
      })
    })

    await page.goto('/')
    await page.getByPlaceholder('Nhập số báo danh (8 chữ số)').fill('99999999')
    await page.getByRole('button', { name: 'Tra cứu' }).click()

    await expect(page.getByText('Không tìm thấy thí sinh với SBD 99999999')).toBeVisible()
  })
})
