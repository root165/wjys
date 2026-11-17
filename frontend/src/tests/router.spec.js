import { describe, expect, it } from 'vitest'

import router from '@/router'

describe('router bootstrap', () => {
  it('registers the initialized public routes', () => {
    const routeNames = router.getRoutes().map((route) => route.name)

    expect(routeNames).toEqual(expect.arrayContaining(['home', 'about']))
  })
})
